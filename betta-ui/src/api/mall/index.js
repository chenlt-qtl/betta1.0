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

export function exchangeProduct(productId, expectedCoinPrice) {
  return request({
    url: '/mall/store/exchanges/' + productId,
    method: 'post',
    data: { expectedCoinPrice }
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

export function batchAddProducts(data) {
  return request({
    url: '/mall/product/batch',
    method: 'post',
    data
  })
}

export function getExchangeRate() {
  return request({
    url: '/mall/product/exchange-rate',
    method: 'get'
  })
}

export function updateExchangeRate(rate) {
  return request({
    url: '/mall/product/exchange-rate',
    method: 'put',
    data: { rate }
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
