package com.ruoyi.ai.component.service;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ContentType;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.aggregator.ContentAggregator;
import dev.langchain4j.rag.content.aggregator.DefaultContentAggregator;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.rag.query.transformer.DefaultQueryTransformer;
import dev.langchain4j.rag.query.transformer.QueryTransformer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static java.util.Collections.*;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toMap;

public class JRetrievalAugmentor implements RetrievalAugmentor {
    private final QueryTransformer queryTransformer;
    private final QueryRouter queryRouter;
    private final ContentAggregator contentAggregator;
    private final ContentInjector contentInjector;
    private final Executor executor;

    public JRetrievalAugmentor(QueryTransformer queryTransformer,
                               QueryRouter queryRouter,
                               ContentAggregator contentAggregator,
                               ContentInjector contentInjector,
                               Executor executor) {
        this.queryTransformer = getOrDefault(queryTransformer, DefaultQueryTransformer::new);
        this.queryRouter = ensureNotNull(queryRouter, "queryRouter");
        this.contentAggregator = getOrDefault(contentAggregator, DefaultContentAggregator::new);
        this.contentInjector = getOrDefault(contentInjector, DefaultContentInjector::new);
        this.executor = getOrDefault(executor, JRetrievalAugmentor::createDefaultExecutor);
    }

    private static ExecutorService createDefaultExecutor() {
        return new ThreadPoolExecutor(
                0, Integer.MAX_VALUE,
                1, SECONDS,
                new SynchronousQueue<>()
        );
    }

    @Override
    public AugmentationResult augment(AugmentationRequest augmentationRequest) {

        ChatMessage chatMessage = augmentationRequest.chatMessage();
        StringBuilder queryText = new StringBuilder();
        if (chatMessage instanceof UserMessage userMessage) {
            if (userMessage.hasSingleText()) {
                queryText = new StringBuilder(userMessage.singleText());
            } else {
                for (dev.langchain4j.data.message.Content content : userMessage.contents()) {
                    if (content.type() == ContentType.TEXT && content instanceof TextContent tc) {
                        queryText.append(tc.text());
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported message type: " + chatMessage.type());
        }
        Query originalQuery = Query.from(queryText.toString(), augmentationRequest.metadata());

        Collection<Query> queries = queryTransformer.transform(originalQuery);

        Map<Query, Collection<List<Content>>> queryToContents = process(queries);

        List<Content> contents = contentAggregator.aggregate(queryToContents);

        ChatMessage augmentedChatMessage = contentInjector.inject(contents, chatMessage);

        return AugmentationResult.builder()
                .chatMessage(augmentedChatMessage)
                .contents(contents)
                .build();
    }

    private Map<Query, Collection<List<Content>>> process(Collection<Query> queries) {
        if (queries.size() == 1) {
            Query query = queries.iterator().next();
            Collection<ContentRetriever> retrievers = queryRouter.route(query);
            if (retrievers.size() == 1) {
                ContentRetriever contentRetriever = retrievers.iterator().next();
                List<Content> contents = contentRetriever.retrieve(query);
                return singletonMap(query, singletonList(contents));
            } else if (retrievers.size() > 1) {
                Collection<List<Content>> contents = retrieveFromAll(retrievers, query).join();
                return singletonMap(query, contents);
            } else {
                return emptyMap();
            }
        } else if (queries.size() > 1) {
            Map<Query, CompletableFuture<Collection<List<Content>>>> queryToFutureContents = new ConcurrentHashMap<>();
            queries.forEach(query -> {
                CompletableFuture<Collection<List<Content>>> futureContents =
                        supplyAsync(() -> queryRouter.route(query), executor)
                                .thenCompose(retrievers -> retrieveFromAll(retrievers, query));
                queryToFutureContents.put(query, futureContents);
            });
            return join(queryToFutureContents);
        } else {
            return emptyMap();
        }
    }

    private CompletableFuture<Collection<List<Content>>> retrieveFromAll(Collection<ContentRetriever> retrievers,
                                                                         Query query) {
        List<CompletableFuture<List<Content>>> futureContents = retrievers.stream()
                .map(retriever -> supplyAsync(() -> retriever.retrieve(query), executor))
                .collect(Collectors.toList());

        return allOf(futureContents.toArray(new CompletableFuture[0]))
                .thenApply(ignored ->
                        futureContents.stream()
                                .map(CompletableFuture::join)
                                .collect(Collectors.toList()));
    }

    private static Map<Query, Collection<List<Content>>> join(
            Map<Query, CompletableFuture<Collection<List<Content>>>> queryToFutureContents) {
        return allOf(queryToFutureContents.values().toArray(new CompletableFuture[0]))
                .thenApply(ignored ->
                        queryToFutureContents.entrySet().stream()
                                .collect(toMap(
                                        Map.Entry::getKey,
                                        entry -> entry.getValue().join()
                                ))
                ).join();
    }

    public static JRetrievalAugmentorBuilder builder() {
        return new JRetrievalAugmentor.JRetrievalAugmentorBuilder();
    }

    public static class JRetrievalAugmentorBuilder {

        private QueryTransformer queryTransformer;
        private QueryRouter queryRouter;
        private ContentAggregator contentAggregator;
        private ContentInjector contentInjector;
        private Executor executor;

        JRetrievalAugmentorBuilder() {
        }

        public JRetrievalAugmentor.JRetrievalAugmentorBuilder contentRetriever(ContentRetriever contentRetriever) {
            this.queryRouter = new DefaultQueryRouter(ensureNotNull(contentRetriever, "contentRetriever"));
            return this;
        }

        public JRetrievalAugmentor.JRetrievalAugmentorBuilder queryTransformer(QueryTransformer queryTransformer) {
            this.queryTransformer = queryTransformer;
            return this;
        }

        public JRetrievalAugmentor.JRetrievalAugmentorBuilder queryRouter(QueryRouter queryRouter) {
            this.queryRouter = queryRouter;
            return this;
        }

        public JRetrievalAugmentor.JRetrievalAugmentorBuilder contentAggregator(ContentAggregator contentAggregator) {
            this.contentAggregator = contentAggregator;
            return this;
        }

        public JRetrievalAugmentor.JRetrievalAugmentorBuilder contentInjector(ContentInjector contentInjector) {
            this.contentInjector = contentInjector;
            return this;
        }

        public JRetrievalAugmentor.JRetrievalAugmentorBuilder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public JRetrievalAugmentor build() {
            return new JRetrievalAugmentor(this.queryTransformer, this.queryRouter, this.contentAggregator, this.contentInjector, this.executor);
        }
    }
}
