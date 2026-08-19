import request from '@/utils/request'

// 查询播放列表列表
export function listPlayList(query) {
  return request({
    url: '/eng/playList/list',
    method: 'get',
    params: query
  })
}

// 查询播放列表详细
export function getPlayList() {
  return request({
    url: '/eng/playList/list/user',
    method: 'get'
  })
}


// 新增播放列表
export function addPlayList(data) {
  return request({
    url: '/eng/playList',
    method: 'post',
    data: data
  })
}

// 修改播放列表
export function updatePlayList(data) {
  return request({
    url: '/eng/playList',
    method: 'put',
    data: data
  })
}

// 删除播放列表
export function delPlayList(id) {
  return request({
    url: '/eng/playList/' + id,
    method: 'delete'
  })
}
