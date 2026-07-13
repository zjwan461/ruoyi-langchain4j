package com.ruoyi.ai.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * GitHub公开Skills仓库同步工具
 * 使用Hutool实现，传入GitHub仓库链接自动下载并提取一级Skill目录到 ./skills
 */
public class GitHubSkillsSyncUtil {

    // 本地skills目录，对应你 Path.of("skills")
    public static final Path LOCAL_SKILLS_ROOT = Path.of("skills");

    /**
     * 入口方法
     *
     * @param githubUrl GitHub仓库地址，示例：https://github.com/langgenius/dify-skills
     */
    public static void syncSkills(String githubUrl, Path skillsRoot) {
        syncSkills(githubUrl, "main", null);
    }

    /**
     * 指定分支同步
     *
     * @param githubUrl GitHub仓库地址
     * @param branch    分支名 main/master
     */
    public static void syncSkills(String githubUrl, String branch, Path skillsRoot) {
        // 1. 解析 github url 提取 owner & repo
        GitHubRepo repoInfo = parseGithubUrl(githubUrl);
        if (repoInfo == null) {
            throw new IllegalArgumentException("GitHub地址解析失败，请检查链接：" + githubUrl);
        }
        String owner = repoInfo.owner;
        String repo = repoInfo.repo;

        // 2. 构造tarball下载地址
        String tarUrl = StrUtil.format("https://github.com/{}/{}/archive/refs/heads/{}.tar.gz",
                owner, repo, branch);
        System.out.println("开始下载源码包：" + tarUrl);

        // 3. 创建临时文件存放压缩包（hutool工具）
        File tempTarFile = FileUtil.createTempFile("skills-", ".tar.gz", true);
        try {
            // Hutool HttpUtil 下载文件
            HttpUtil.downloadFile(tarUrl, tempTarFile);
            System.out.println("压缩包下载完成，临时文件：" + tempTarFile.getAbsolutePath());

            // 4. 确保本地skills目录存在
            FileUtil.mkdir(LOCAL_SKILLS_ROOT.toFile());

            // tar解压后顶层文件夹名称格式 {repo}-{branch}/
            String tarRootPrefix = repo + "-" + branch + "/";

            // 5. 遍历tar，只复制仓库一级子文件夹（各个skill）
            try (InputStream fis = FileUtil.getInputStream(tempTarFile);
                 GzipCompressorInputStream gzIn = new GzipCompressorInputStream(fis);
                 TarArchiveInputStream tarIn = new TarArchiveInputStream(gzIn)) {

                TarArchiveEntry entry;
                while ((entry = tarIn.getNextTarEntry()) != null) {
                    String entryName = entry.getName();
                    // 只处理仓库根目录内文件
                    if (!entryName.startsWith(tarRootPrefix)) {
                        continue;
                    }
                    String relativePath = StrUtil.subAfter(entryName, tarRootPrefix, false);
                    if (StrUtil.isBlank(relativePath)) {
                        continue;
                    }
                    // 只保留第一层目录下内容（skill文件夹）
                    int firstSlash = relativePath.indexOf("/");
                    if (firstSlash <= 0) {
                        continue;
                    }
                    String skillFolder = StrUtil.subBefore(relativePath, "/", false);
                    String targetRelative = StrUtil.subAfter(relativePath, skillFolder + "/", false);
                    Path targetPath = null;
                    if (skillsRoot == null) {
                        targetPath = LOCAL_SKILLS_ROOT.resolve(skillFolder).resolve(targetRelative);
                    } else {
                        targetPath = skillsRoot.resolve(skillFolder).resolve(targetRelative);
                    }

                    if (entry.isDirectory()) {
                        FileUtil.mkdir(targetPath.toFile());
                    } else {
                        // 使用hutool流拷贝写入文件
                        FileUtil.writeFromStream(tarIn, targetPath.toFile());
                        System.out.println("已写入：" + targetPath);
                    }
                }
            }
            System.out.println("\n✅ Skills同步完成！本地目录：" + LOCAL_SKILLS_ROOT.toAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("同步skills失败", e);
        } finally {
            // 删除临时压缩文件
            FileUtil.del(tempTarFile);
        }
    }

    /**
     * 解析github仓库url
     * 支持格式：
     * https://github.com/A/B
     * https://github.com/A/B.git
     */
    private static GitHubRepo parseGithubUrl(String url) {
        url = StrUtil.trim(url);
        // 移除.git后缀
        url = StrUtil.removeSuffix(url, ".git");
        String[] split = url.split("/");
        // 寻找 github.com 后的两段 owner/repo
        int idx = -1;
        for (int i = 0; i < split.length; i++) {
            if ("github.com".equals(split[i])) {
                idx = i;
                break;
            }
        }
        if (idx == -1 || idx + 2 >= split.length) {
            return null;
        }
        String owner = split[idx + 1];
        String repo = split[idx + 2];
        return new GitHubRepo(owner, repo);
    }

    private static class GitHubRepo {
        String owner;
        String repo;

        GitHubRepo(String owner, String repo) {
            this.owner = owner;
            this.repo = repo;
        }
    }

    // ========== 测试Main ==========
    public static void main(String[] args) {
        // 修改为你的公开skills仓库地址
        String gitUrl = "https://github.com/MiniMax-AI/skills";
        syncSkills(gitUrl, null);
    }
}