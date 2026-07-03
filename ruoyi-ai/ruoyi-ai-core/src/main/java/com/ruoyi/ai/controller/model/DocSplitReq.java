package com.ruoyi.ai.controller.model;

import java.util.LinkedHashMap;
import java.util.List;

public class DocSplitReq {

  private int maxSegmentSize;

  private int maxOverlapSize;

  private List<LinkedHashMap<String, Object>> fileList;

  public int getMaxSegmentSize() {
    return maxSegmentSize;
  }

  public void setMaxSegmentSize(int maxSegmentSize) {
    this.maxSegmentSize = maxSegmentSize;
  }

  public int getMaxOverlapSize() {
    return maxOverlapSize;
  }

  public void setMaxOverlapSize(int maxOverlapSize) {
    this.maxOverlapSize = maxOverlapSize;
  }

  public List<LinkedHashMap<String, Object>> getFileList() {
    return fileList;
  }

  public void setFileList(
      List<LinkedHashMap<String, Object>> fileList) {
    this.fileList = fileList;
  }
}
