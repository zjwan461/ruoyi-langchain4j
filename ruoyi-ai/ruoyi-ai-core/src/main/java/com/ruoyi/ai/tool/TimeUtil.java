package com.ruoyi.ai.tool;

import dev.langchain4j.agent.tool.Tool;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 时间工具类
 * 功能覆盖：
 * 1. 获取当前时间（多种格式）
 * 2. 获取时间戳（秒/毫秒）
 * 3. 时间戳转日期时间
 * 4. 时区互相转换
 * 5. 根据日期计算星期几
 */
public class TimeUtil {
    // 标准日期格式化器（本地时区）
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ===================== 1. 获取当前时间 =====================

    /**
     * 获取当前系统时间，格式 yyyy-MM-dd HH:mm:ss
     */
    @Tool
    public String getCurrentTime() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }

    /**
     * 获取当前时间，自定义格式
     */
    public String getCurrentTime(String pattern) {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 获取当前 LocalDateTime 对象
     */
    @Tool
    public LocalDateTime getCurrentLocalDateTime() {
        return LocalDateTime.now();
    }

    // ===================== 2. 获取时间戳 =====================

    /**
     * 获取当前毫秒时间戳（13位）
     */
    @Tool
    public long getCurrentMilliTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 获取当前秒时间戳（10位）
     */
    @Tool
    public long getCurrentSecondTimestamp() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * 指定日期字符串转毫秒时间戳
     *
     * @param dateStr 日期字符串 yyyy-MM-dd HH:mm:ss
     */
    @Tool
    public long getMilliTimestamp(String dateStr) {
        LocalDateTime localDateTime = LocalDateTime.parse(dateStr, DATE_TIME_FORMATTER);
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 指定日期字符串转秒时间戳
     */
    @Tool
    public long getSecondTimestamp(String dateStr) {
        return getMilliTimestamp(dateStr) / 1000;
    }

    // ===================== 3. 时间戳转换 =====================

    /**
     * 毫秒时间戳转标准日期字符串 yyyy-MM-dd HH:mm:ss
     */
    @Tool
    public String timestampToDateTime(long milliTs) {
        Instant instant = Instant.ofEpochMilli(milliTs);
        LocalDateTime dateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    /**
     * 秒时间戳转标准日期字符串
     */
    @Tool
    public String secondTsToDateTime(long secondTs) {
        return timestampToDateTime(secondTs * 1000);
    }

    /**
     * 时间戳转LocalDateTime
     */
    @Tool
    public LocalDateTime timestampToLocalDateTime(long milliTs) {
        return Instant.ofEpochMilli(milliTs).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    // ===================== 4. 时区转换 =====================

    /**
     * 时区转换
     *
     * @param sourceDateTime 源时间字符串 yyyy-MM-dd HH:mm:ss
     * @param sourceZoneId   源时区 例：Asia/Shanghai、UTC、America/New_York
     * @param targetZoneId   目标时区
     * @return 转换后时间字符串
     */
    @Tool
    public String convertTimeZone(String sourceDateTime, String sourceZoneId, String targetZoneId) {
        LocalDateTime sourceLocal = LocalDateTime.parse(sourceDateTime, DATE_TIME_FORMATTER);
        ZonedDateTime sourceZoned = ZonedDateTime.of(sourceLocal, ZoneId.of(sourceZoneId));
        ZonedDateTime targetZoned = sourceZoned.withZoneSameInstant(ZoneId.of(targetZoneId));
        return targetZoned.format(DATE_TIME_FORMATTER);
    }

    /**
     * 本地时区时间转UTC
     */
    @Tool
    public String localToUtc(String localDateTime) {
        return convertTimeZone(localDateTime, ZoneId.systemDefault().getId(), "UTC");
    }

    /**
     * UTC时间转本地时区
     */
    @Tool
    public String utcToLocal(String utcDateTime) {
        return convertTimeZone(utcDateTime, "UTC", ZoneId.systemDefault().getId());
    }

    // ===================== 5. 星期几计算器 =====================

    /**
     * 根据日期字符串获取星期数字 1=周一 ... 7=周日
     */
    @Tool
    public int getWeekNum(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
        return date.getDayOfWeek().getValue();
    }

    /**
     * 根据日期字符串获取中文星期
     */
    @Tool
    public String getWeekCn(String dateStr) {
        int weekNum = getWeekNum(dateStr);
        return switch (weekNum) {
            case 1 -> "星期一";
            case 2 -> "星期二";
            case 3 -> "星期三";
            case 4 -> "星期四";
            case 5 -> "星期五";
            case 6 -> "星期六";
            case 7 -> "星期日";
            default -> "未知";
        };
    }

    /**
     * 根据时间戳获取中文星期
     */
    @Tool
    public String getWeekCnByTs(long milliTs) {
        String dateTime = timestampToDateTime(milliTs);
        return getWeekCn(dateTime.substring(0, 10));
    }

    // 兼容旧 Date 工具方法（可选）
    @Tool
    public Date strToDate(String dateStr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.parse(dateStr);
    }

    @Tool
    public String dateToStr(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }
}