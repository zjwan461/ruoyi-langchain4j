package com.ruoyi.ai.util;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.http.HttpUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
public class ModelScopeUtil {

    private static final int BUFFER_SIZE = 4096;

    private static final String BLOB_FILE_TYPE = "blob";
    private static final String TREE_FILE_TYPE = "tree";
    private static final String DEFAULT_REVISION = "master";

    private static final String urlTemp = "https://modelscope.cn/api/v1/models/%s/repo/files?Revision%s&Root=%s";

    private static final String downloadUrlTemp = "https://modelscope.cn/models/%s/resolve/%s/%s";


    public static void downloadFile(String fileUrl, String savePath) throws IOException {
        URL url = new URL(fileUrl);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestMethod("GET");

        // 检查本地文件是否存在，获取已下载的字节数
        File file = new File(savePath);
        long downloadedBytes = 0;
        // 检查父文件夹是否存在，不存在则创建
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (file.exists()) {
            downloadedBytes = file.length();
            // 设置 Range 请求头，从已下载的位置继续下载
            httpConn.setRequestProperty("Range", "bytes=" + downloadedBytes + "-");
        }

        int responseCode = httpConn.getResponseCode();

        // 检查响应状态码
        if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL) {
            // 获取文件的总大小
            long fileSize = httpConn.getContentLengthLong() + downloadedBytes;

            // 打开输入流和输出流
            try (InputStream inputStream = httpConn.getInputStream();
                 RandomAccessFile outputStream = new RandomAccessFile(file, "rw")) {

                // 将文件指针移动到已下载的位置
                outputStream.seek(downloadedBytes);

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    downloadedBytes += bytesRead;

                    // 打印下载进度
                    double progress = (double) downloadedBytes / fileSize * 100;
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("%s Download progress: %.2f%%\n", file.getName(), progress));
                    }
                }
            }
            log.info("File:{} downloaded successfully.", file.getName());
        } else {
            log.error("Error: HTTP response code {} ", responseCode);
        }
        httpConn.disconnect();
    }


    public static List<ModelScopeFile> listModelFilesList(String repoId, String revision, String root, List<ModelScopeFile> list) {
        String url = String.format(urlTemp, repoId, revision, root);
        String resp = HttpUtils.sendGet(url);
        JSONObject jobj = JSONUtil.parseObj(resp);
        JSONArray array = (JSONArray) jobj.getByPath("Data.Files");
        array.forEach(item -> {
            ModelScopeFile file = new ModelScopeFile();
            JSONObject it = (JSONObject) item;
            file.setName(it.getStr("Name"));
            file.setType(it.getStr("Type"));
            file.setPath(it.getStr("Path"));
            file.setSize(it.getLong("Size"));
            if (file.getType().equals(BLOB_FILE_TYPE)) {
                file.setDownloadUri(String.format(downloadUrlTemp, repoId, revision, file.getPath()));
                list.add(file);
            } else {
                listModelFilesList(repoId, revision, file.getPath(), list);
            }
        });
        return list;
    }

    public static List<ModelScopeFile> listModelFilesTree(String repoId, String revision, String root) {
        String url = String.format(urlTemp, repoId, revision, root);
        String resp = HttpUtils.sendGet(url);
        JSONObject jobj = JSONUtil.parseObj(resp);
        JSONArray array = (JSONArray) jobj.getByPath("Data.Files");
        List<ModelScopeFile> list = new ArrayList<>();
        array.forEach(item -> {
            ModelScopeFile file = new ModelScopeFile();
            JSONObject it = (JSONObject) item;
            file.setName(it.getStr("Name"));
            file.setType(it.getStr("Type"));
            file.setPath(it.getStr("Path"));
            file.setSize(it.getLong("Size"));
            if (file.getType().equals(BLOB_FILE_TYPE)) {
                file.setDownloadUri(String.format(downloadUrlTemp, repoId, revision, file.getPath()));
            } else {
                file.setChildren(listModelFilesTree(repoId, revision, file.getPath()));
            }
            list.add(file);
        });
        return list;
    }

    public static String downloadMultiThread(String repoId, String saveDir, String allowFilePattern, int threadNum) {
        File root = new File(saveDir);
        if (!root.exists()) {
            root.mkdirs();
        }
        List<ModelScopeFile> list = listModelFilesList(repoId, DEFAULT_REVISION, "", new ArrayList<>());
        list = list.stream()
                .filter(item -> item.getName().matches(allowFilePattern))
                .sorted((o1, o2) -> Math.toIntExact(o1.getSize() - o2.getSize()))
                .collect(Collectors.toList());
        int coreSize = Runtime.getRuntime().availableProcessors();
        threadNum = Math.max(threadNum, coreSize);
        ThreadPoolExecutor threadPool = ThreadUtil.newExecutor(coreSize, threadNum);
        CountDownLatch countDownLatch = new CountDownLatch(list.size());
        for (ModelScopeFile file : list) {
            if (file.getPath().contains("/")) {
                File f = new File(saveDir + "/" + file.getPath());
                if (!f.getParentFile().exists()) {
                    f.getParentFile().mkdirs();
                }
            }
            threadPool.execute(() -> {
                try {
                    downloadFile(file.getDownloadUri(), saveDir + "/" + file.getPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    countDownLatch.countDown();
                }

            });
        }
        try {
            countDownLatch.await();
            threadPool.shutdown();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return root.getAbsolutePath();
    }

    public static String download(String repoId, String saveDir, String allowFilePattern) {
        List<ModelScopeFile> list = listModelFilesTree(repoId, DEFAULT_REVISION, "");
        download(saveDir, allowFilePattern, list);
        return new File(saveDir).getAbsolutePath();
    }

    private static void download(String saveDir, String allowFilePattern, List<ModelScopeFile> list) {
        File saveDirRoot = new File(saveDir);
        if (!saveDirRoot.exists()) {
            saveDirRoot.mkdirs();
        }
        for (ModelScopeFile file : list) {
            if (file.getType().equals(BLOB_FILE_TYPE)) {
                if (StringUtils.isNotBlank(allowFilePattern) && !file.getName().matches(allowFilePattern)) {
                    continue;
                }
                try {
                    downloadFile(file.getDownloadUri(), saveDir + "/" + file.getName());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                File dir = new File(saveDir + "/" + file.getPath());
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                download(saveDir + "/" + file.getPath(), allowFilePattern, file.getChildren());
            }
        }
    }

    @Data
    public static class ModelScopeFile {
        private String name;

        private Long size;

        private String type;

        private String path;

        private String downloadUri;

        private List<ModelScopeFile> children;
    }

    public static void main(String[] args) {
//        List<ModelScopeFile> list = listModelFiles("zjwan461/shibing624_text2vec-base-chinese", DEFAULT_REVISION, "");
//        System.out.println(JSONUtil.toJsonStr(list));
//
//        String saveDir = download("zjwan461/shibing624_text2vec-base-chinese", "./models", "[\\s\\S]*");
//        System.out.println("save model file to: " + saveDir);
//        List<ModelScopeFile> list = listModelFilesList("zjwan461/shibing624_text2vec-base-chinese", DEFAULT_REVISION, "", new ArrayList<>());
//        list = list.stream().sorted((o1, o2) -> Math.toIntExact(o1.getSize() - o2.getSize())).collect(Collectors.toList());
//
//        System.out.println(JSONUtil.toJsonStr(list));
        String saveDir = downloadMultiThread("zjwan461/shibing624_text2vec-base-chinese", "./models", "[\\s\\S]*", 4);
        System.out.println("save model file to: " + saveDir);
    }
}
