import request from '@/utils/request'

// FormData 无法用于现有的请求体去重；批量录入逐件提交时应允许连续请求。
function multipart(part, value, image) {
  const data = new FormData()
  data.append(part, new Blob([JSON.stringify(value)], { type: 'application/json' }))
  if (image) data.append('image', image)
  return data
}

export function listItems(params) {
  return request({ url: '/wardrobe/items', method: 'get', params })
}

export function getItem(id) {
  return request({ url: `/wardrobe/items/${id}`, method: 'get' })
}

export function addItem(item, image) {
  return request({ url: '/wardrobe/items', method: 'post', data: multipart('item', item, image), headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false } })
}

export function updateItem(id, item, image) {
  return request({ url: `/wardrobe/items/${id}`, method: 'put', data: multipart('item', item, image), headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false } })
}

export function deleteItem(id) {
  return request({ url: `/wardrobe/items/${id}`, method: 'delete' })
}

export function listOutfits(params) {
  return request({ url: '/wardrobe/outfits', method: 'get', params })
}

export function getOutfit(id) {
  return request({ url: `/wardrobe/outfits/${id}`, method: 'get' })
}

export function addOutfit(outfit, image) {
  return request({ url: '/wardrobe/outfits', method: 'post', data: multipart('outfit', outfit, image), headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false } })
}

export function updateOutfit(id, outfit, image) {
  return request({ url: `/wardrobe/outfits/${id}`, method: 'put', data: multipart('outfit', outfit, image), headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false } })
}

export function deleteOutfit(id) {
  return request({ url: `/wardrobe/outfits/${id}`, method: 'delete' })
}

export function setFavorite(id, favorite) {
  return request({ url: `/wardrobe/outfits/${id}/favorite`, method: 'put', data: { favorite } })
}

export function moveOutfit(id, direction) {
  return request({ url: `/wardrobe/outfits/${id}/position`, method: 'put', data: { direction }, headers: { repeatSubmit: false } })
}

export function getRecommendations(params) {
  return request({ url: '/wardrobe/recommendations', method: 'get', params })
}

// 二进制请求可能返回登录失效的 JSON，不能把它当作图片展示。
export function getWardrobeImage(kind, id) {
  return request({ url: `/wardrobe/${kind}/${id}/image`, method: 'get', responseType: 'blob' }).then(blob => {
    if (!(blob instanceof Blob) || !/^image\/(jpeg|png)$/i.test(blob.type)) {
      throw new Error('图片不可用')
    }
    return blob
  })
}
