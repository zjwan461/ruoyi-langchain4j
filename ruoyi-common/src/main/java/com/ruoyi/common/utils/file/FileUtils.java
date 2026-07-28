package com.ruoyi.common.utils.file;

import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 文件处理工具类
 *
 * @author ruoyi
 */
public class FileUtils {
    public static String FILENAME_PATTERN = "[a-zA-Z0-9_\\-\\|\\.\\u4e00-\\u9fa5]+";

    /**
     * 输出指定文件的byte数组
     *
     * @param filePath 文件路径
     * @param os       输出流
     * @return
     */
    public static void writeBytes(String filePath, OutputStream os) throws IOException {
        FileInputStream fis = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new FileNotFoundException(filePath);
            }
            fis = new FileInputStream(file);
            byte[] b = new byte[1024];
            int length;
            while ((length = fis.read(b)) > 0) {
                os.write(b, 0, length);
            }
        } catch (IOException e) {
            throw e;
        } finally {
            IOUtils.close(os);
            IOUtils.close(fis);
        }
    }

    /**
     * 写数据到文件中
     *
     * @param data 数据
     * @return 目标文件
     * @throws IOException IO异常
     */
    public static String writeImportBytes(byte[] data) throws IOException {
        return writeBytes(data, RuoYiConfig.getImportPath());
    }

    /**
     * 写数据到文件中
     *
     * @param data      数据
     * @param uploadDir 目标文件
     * @return 目标文件
     * @throws IOException IO异常
     */
    public static String writeBytes(byte[] data, String uploadDir) throws IOException {
        FileOutputStream fos = null;
        String pathName = "";
        try {
            String extension = getFileExtendName(data);
            pathName = DateUtils.datePath() + "/" + IdUtils.fastUUID() + "." + extension;
            File file = FileUploadUtils.getAbsoluteFile(uploadDir, pathName);
            fos = new FileOutputStream(file);
            fos.write(data);
        } finally {
            IOUtils.close(fos);
        }
        return FileUploadUtils.getPathFileName(uploadDir, pathName);
    }

    /**
     * 移除路径中的请求前缀片段
     *
     * @param filePath 文件路径
     * @return 移除后的文件路径
     */
    public static String stripPrefix(String filePath) {
        return StringUtils.substringAfter(filePath, Constants.RESOURCE_PREFIX);
    }

    /**
     * 删除文件
     *
     * @param filePath 文件
     * @return
     */
    public static boolean deleteFile(String filePath) {
        boolean flag = false;
        File file = new File(filePath);
        // 路径为文件且不为空则进行删除
        if (file.isFile() && file.exists()) {
            flag = file.delete();
        }
        return flag;
    }

    /**
     * 文件名称验证
     *
     * @param filename 文件名称
     * @return true 正常 false 非法
     */
    public static boolean isValidFilename(String filename) {
        return filename.matches(FILENAME_PATTERN);
    }

    /**
     * 检查文件是否可下载
     *
     * @param resource 需要下载的文件
     * @return true 正常 false 非法
     */
    public static boolean checkAllowDownload(String resource) {
        // 禁止目录上跳级别
        if (StringUtils.contains(resource, "..")) {
            return false;
        }

        // 检查允许下载的文件规则
        if (ArrayUtils.contains(MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION, FileTypeUtils.getFileType(resource))) {
            return true;
        }

        // 不在允许下载的文件规则
        return false;
    }

    /**
     * 下载文件名重新编码
     *
     * @param request  请求对象
     * @param fileName 文件名
     * @return 编码后的文件名
     */
    public static String setFileDownloadHeader(HttpServletRequest request, String fileName) throws UnsupportedEncodingException {
        final String agent = request.getHeader("USER-AGENT");
        String filename = fileName;
        if (agent.contains("MSIE")) {
            // IE浏览器
            filename = URLEncoder.encode(filename, "utf-8");
            filename = filename.replace("+", " ");
        } else if (agent.contains("Firefox")) {
            // 火狐浏览器
            filename = new String(fileName.getBytes(), "ISO8859-1");
        } else if (agent.contains("Chrome")) {
            // google浏览器
            filename = URLEncoder.encode(filename, "utf-8");
        } else {
            // 其它浏览器
            filename = URLEncoder.encode(filename, "utf-8");
        }
        return filename;
    }

    /**
     * 下载文件名重新编码
     *
     * @param response     响应对象
     * @param realFileName 真实文件名
     */
    public static void setAttachmentResponseHeader(HttpServletResponse response, String realFileName) throws UnsupportedEncodingException {
        String percentEncodedFileName = percentEncode(realFileName);

        StringBuilder contentDispositionValue = new StringBuilder();
        contentDispositionValue.append("attachment; filename=")
                .append(percentEncodedFileName)
                .append(";")
                .append("filename*=")
                .append("utf-8''")
                .append(percentEncodedFileName);

        response.addHeader("Access-Control-Expose-Headers", "Content-Disposition,download-filename");
        response.setHeader("Content-disposition", contentDispositionValue.toString());
        response.setHeader("download-filename", percentEncodedFileName);
    }

    /**
     * 百分号编码工具方法
     *
     * @param s 需要百分号编码的字符串
     * @return 百分号编码后的字符串
     */
    public static String percentEncode(String s) throws UnsupportedEncodingException {
        String encode = URLEncoder.encode(s, StandardCharsets.UTF_8.toString());
        return encode.replaceAll("\\+", "%20");
    }

    /**
     * 获取图像后缀
     *
     * @param photoByte 图像数据
     * @return 后缀名
     */
    public static String getFileExtendName(byte[] photoByte) {
        String strFileExtendName = "jpg";
        if ((photoByte[0] == 71) && (photoByte[1] == 73) && (photoByte[2] == 70) && (photoByte[3] == 56)
                && ((photoByte[4] == 55) || (photoByte[4] == 57)) && (photoByte[5] == 97)) {
            strFileExtendName = "gif";
        } else if ((photoByte[6] == 74) && (photoByte[7] == 70) && (photoByte[8] == 73) && (photoByte[9] == 70)) {
            strFileExtendName = "jpg";
        } else if ((photoByte[0] == 66) && (photoByte[1] == 77)) {
            strFileExtendName = "bmp";
        } else if ((photoByte[1] == 80) && (photoByte[2] == 78) && (photoByte[3] == 71)) {
            strFileExtendName = "png";
        }
        return strFileExtendName;
    }

    /**
     * 获取文件名称 /profile/upload/2022/04/16/ruoyi.png -- ruoyi.png
     *
     * @param fileName 路径名称
     * @return 没有文件路径的名称
     */
    public static String getName(String fileName) {
        if (fileName == null) {
            return null;
        }
        int lastUnixPos = fileName.lastIndexOf('/');
        int lastWindowsPos = fileName.lastIndexOf('\\');
        int index = Math.max(lastUnixPos, lastWindowsPos);
        return fileName.substring(index + 1);
    }

    /**
     * 获取不带后缀文件名称 /profile/upload/2022/04/16/ruoyi.png -- ruoyi
     *
     * @param fileName 路径名称
     * @return 没有文件路径和后缀的名称
     */
    public static String getNameNotSuffix(String fileName) {
        if (fileName == null) {
            return null;
        }
        String baseName = FilenameUtils.getBaseName(fileName);
        return baseName;
    }

    public static String getFileExt(String fileName) {
        if (fileName == null) {
            return null;
        }
        return FilenameUtils.getExtension(fileName);
    }


    // 常见图片后缀（小写）
    private static final Set<String> IMAGE_SUFFIX_SET = new HashSet<>(
            Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "tif", "svg", "heic", "heif", "ico")
    );

    /**
     * 根据文件名后缀判断是否为图片
     *
     * @param fileName 完整文件名（如 test.png、/upload/a.jpg）
     * @return true=图片
     */
    public static boolean isImageBySuffix(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        // 没有后缀 或者 .在第一个字符（如 .gitignore）
        if (lastDotIndex <= 0) {
            return false;
        }
        // 提取后缀转小写
        String suffix = fileName.substring(lastDotIndex + 1).toLowerCase();
        return IMAGE_SUFFIX_SET.contains(suffix);
    }

    // 图片后缀 -> mime类型映射
    private static final Map<String, String> IMAGE_MIME_MAP;

    static {
        IMAGE_MIME_MAP = new HashMap<>();
        IMAGE_MIME_MAP.put("jpg", "image/jpeg");
        IMAGE_MIME_MAP.put("jpeg", "image/jpeg");
        IMAGE_MIME_MAP.put("png", "image/png");
        IMAGE_MIME_MAP.put("gif", "image/gif");
        IMAGE_MIME_MAP.put("bmp", "image/bmp");
        IMAGE_MIME_MAP.put("webp", "image/webp");
        IMAGE_MIME_MAP.put("svg", "image/svg+xml");
        IMAGE_MIME_MAP.put("ico", "image/x-icon");
        IMAGE_MIME_MAP.put("tiff", "image/tiff");
        IMAGE_MIME_MAP.put("tif", "image/tiff");
    }

    /**
     * 读取图片文件，返回完整 dataURL（data:image/xxx;base64,xxxx）
     *
     * @param filePath 文件完整路径
     * @return 可直接给img.src使用的dataUrl
     * @throws IOException 文件读取异常 / 非支持图片格式抛出
     */
    public static String readImageToDataBase64(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        byte[] bytes = Files.readAllBytes(path);
        String fileName = path.getFileName().toString();

        // 提取后缀
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot <= 0) {
            throw new IOException("无法识别文件后缀，不是合法图片：" + filePath);
        }
        String suffix = fileName.substring(lastDot + 1).toLowerCase();
        String mime = IMAGE_MIME_MAP.get(suffix);
        if (mime == null) {
            throw new IOException("不支持的图片格式[" + suffix + "]，路径：" + filePath);
        }

        String base64 = Base64.getEncoder().encodeToString(bytes);
        return String.format("data:%s;base64,%s", mime, base64);
    }

    /**
     * 兼容兜底版本：非图片不抛异常，只返回原生base64（按需选用）
     */
    public static String readFileToBase64WithPrefix(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        byte[] bytes = Files.readAllBytes(path);
        String fileName = path.getFileName().toString();
        String base64 = Base64.getEncoder().encodeToString(bytes);

        int lastDot = fileName.lastIndexOf('.');
        if (lastDot <= 0) {
            return base64;
        }
        String suffix = fileName.substring(lastDot + 1).toLowerCase();
        String mime = IMAGE_MIME_MAP.get(suffix);
        if (mime != null) {
            return String.format("data:%s;base64,%s", mime, base64);
        }
        // 不是图片，直接返回纯净base64
        return base64;
    }


}
