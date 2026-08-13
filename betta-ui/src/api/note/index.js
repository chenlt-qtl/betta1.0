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

// 将多个笔记文件或单个文件夹移动到目标目录
export function moveNoteFiles(data) {
  return request({
    url: '/system/note/file/move',
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

export function getFavoriteNotes() {
  return request({
    url: '/system/note/favorites',
    method: 'get'
  })
}

export function updateNoteFavorite(data) {
  return request({
    url: '/system/note/favorite',
    method: 'put',
    data
  })
}

export function getJournalSettings() {
  return request({
    url: '/system/note/journal/settings',
    method: 'get'
  })
}

export function updateJournalSettings(data) {
  return request({
    url: '/system/note/journal/settings',
    method: 'put',
    data
  })
}

export function openTodayJournal() {
  return request({
    url: '/system/note/journal/today',
    method: 'post'
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
