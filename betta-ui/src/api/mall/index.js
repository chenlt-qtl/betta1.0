import request from '@/utils/request'

export function listStoreProducts(query) {
  return request({
    url: '/mall/store/products',
    method: 'get',
    params: query
  })
}

export function getStoreProduct(id) {
  return request({
    url: '/mall/store/products/' + id,
    method: 'get'
  })
}

export function getStoreBalance() {
  return request({
    url: '/mall/store/balance',
    method: 'get'
  })
}

export function exchangeProduct(productId) {
  return request({
    url: '/mall/store/exchanges/' + productId,
    method: 'post'
  })
}

export function listMyExchanges(query) {
  return request({
    url: '/mall/store/exchanges',
    method: 'get',
    params: query
  })
}

export function listProducts(query) {
  return request({
    url: '/mall/product/list',
    method: 'get',
    params: query
  })
}

export function getProduct(id) {
  return request({
    url: '/mall/product/' + id,
    method: 'get'
  })
}

export function addProduct(data) {
  return request({
    url: '/mall/product',
    method: 'post',
    data
  })
}

export function updateProduct(data) {
  return request({
    url: '/mall/product',
    method: 'put',
    data
  })
}

export function delProduct(ids) {
  return request({
    url: '/mall/product/' + ids,
    method: 'delete'
  })
}

export function listExchanges(query) {
  return request({
    url: '/mall/exchange/list',
    method: 'get',
    params: query
  })
}
