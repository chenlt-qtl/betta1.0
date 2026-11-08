export const CATEGORIES = ['上衣', '下装', '外套', '连衣裙', '家居服', '鞋', '包', '配饰']
export const SEASONS = ['春', '夏', '秋', '冬']
export const OCCASIONS = ['通勤', '休闲', '约会', '运动', '居家']

export function dateKey(date) {
  const pad = value => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export function validImage(file) {
  return file && (/^image\/(jpeg|png)$/i.test(file.type) || (!file.type && /\.(jpe?g|png)$/i.test(file.name))) && file.size > 0 && file.size <= 5 * 1024 * 1024
}
