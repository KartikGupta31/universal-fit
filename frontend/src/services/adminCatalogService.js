import { getAccessToken } from './authService'

async function request(path, options = {}) {
    const token = getAccessToken()
    const response = await fetch(path, {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
            ...options.headers,
        },
    })
    const text = await response.text()
    let data = {}
    if (text) { try { data = JSON.parse(text) } catch { data = { message: text } } }
    if (!response.ok) throw new Error(data.message || data.error || data.detail || `Request failed (${response.status})`)
    return data
}

export const getAdminBrands = () => request('/api/admin/data/brands')
export const createAdminBrand = (payload) => request('/api/admin/data/brands', { method: 'POST', body: JSON.stringify(payload) })
export const updateAdminBrand = (id, payload) => request(`/api/admin/data/brands/${id}`, { method: 'PUT', body: JSON.stringify(payload) })
export const setAdminBrandActive = (id, active) => request(`/api/admin/data/brands/${id}/active`, { method: 'PATCH', body: JSON.stringify({ active }) })
export const getAdminCharts = (brandId = '') => request(`/api/admin/data/charts${brandId ? `?brandId=${encodeURIComponent(brandId)}` : ''}`)
export const createAdminChart = (payload) => request('/api/admin/data/charts', { method: 'POST', body: JSON.stringify(payload) })
export const updateAdminChart = (id, payload) => request(`/api/admin/data/charts/${id}`, { method: 'PUT', body: JSON.stringify(payload) })
export const setAdminChartVerified = (id, verified) => request(`/api/admin/data/charts/${id}/verification`, { method: 'PATCH', body: JSON.stringify({ verified }) })
export const setAdminChartActive = (id, active) => request(`/api/admin/data/charts/${id}/active`, { method: 'PATCH', body: JSON.stringify({ active }) })
