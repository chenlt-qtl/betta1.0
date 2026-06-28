import request from '@/utils/request'

export function getNoteTree() {
  return request({
    url: '/system/note/tree',
    method: 'get'
  })
}

export function getNoteContent(path) {
  return request({
    url: '/system/note/content',
    method: 'get',
    params: { path }
  })
}

export function saveNoteContent(data) {
  return request({
    url: '/system/note/content',
    method: 'put',
    data
  })
}

export function createNoteFile(data) {
  return request({
    url: '/system/note/file',
    method: 'post',
    data
  })
}

export function renameNoteFile(data) {
  return request({
    url: '/system/note/file',
    method: 'put',
    data
  })
}

export function deleteNoteFile(path) {
  return request({
    url: '/system/note/file',
    method: 'delete',
    params: { path }
  })
}

export function searchNotes(keyword) {
  return request({
    url: '/system/note/search',
    method: 'get',
    params: { keyword }
  })
}

export function uploadNoteImage(file, notePath) {
  const data = new FormData()
  data.append('file', file)
  data.append('notePath', notePath)
  return request({
    url: '/system/note/upload-image',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function downloadNoteFile(path) {
  return request({
    url: '/system/note/file/download',
    method: 'get',
    params: { path },
    responseType: 'blob'
  })
}
