/*
 * @Author: 苏犇 suben@noemail.com
 * @Date: 2025-08-20 09:30:20
 * @FilePath: \ruoyi-ui\src\api\home\home.js
 * @Description: 
 * 
 * Copyright (c) 2025 by ${git_name_email}, All Rights Reserved. 
 */
import request from '@/utils/request'

// 查询论坛帖子列表
export function queryMyTask () {
  return request({
    url: '/requirement/task/my',
    method: 'get'
  })
}

// 查询论坛帖子详细
export function queryRecentTask () {
  return request({
    url: '/requirement/task/recent',
    method: 'get'
  })
}
