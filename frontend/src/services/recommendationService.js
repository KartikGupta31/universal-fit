
import { getAccessToken } from './authService'

const API_URL = '/api'

async function apiRequest(endpoint, options = {}) {
    const token = getAccessToken()

    const response = await fetch(`${API_URL}${endpoint}`, {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
            ...options.headers,
        },
    })

    if (response.status === 204) return null

    const responseText = await response.text()

    let data = {}

    if (responseText) {
        try {
            data = JSON.parse(responseText)
        } catch {
            data = { message: responseText }
        }
    }

    if (!response.ok) {
        throw new Error(
            data.message ||
            data.error ||
            `Request failed with status ${response.status}`
        )
    }

    return data
}

export async function getProducts(page = 0, size = 50) {
    return apiRequest(`/products?page=${page}&size=${size}`)
}

export async function getBrands(page = 0, size = 50) {
    return apiRequest(`/brands?page=${page}&size=${size}`)
}

export async function getCategoryRecommendation(brandId, category, region) {
    const regionQuery = region ? `?region=${encodeURIComponent(region)}` : ''
    return apiRequest(`/recommendations/brand/${brandId}/category/${category}${regionQuery}`, { method: 'POST' })
}

export async function getRecommendation(productId) {
    return apiRequest(`/recommendations/${productId}`, {
        method: 'POST',
    })
}
