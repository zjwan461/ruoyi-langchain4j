import request from '@/utils/request'

//获取验证图片
export function reqGet(data) {
    return request({
        url: '/captcha/get',
        method: 'post',
        data
    })
}

//滑动或者点选验证
export function reqCheck(data) {
    return request({
        url: '/captcha/check',
        method: 'post',
        data
    })
}
