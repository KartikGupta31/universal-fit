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
    if (text) {
        try { data = JSON.parse(text) } catch { data = { message: text } }
    }
    if (!response.ok) {
        throw new Error(data.detail || data.message || data.error || `Request failed (${response.status})`)
    }
    return data
}

export function getAdminDashboard() {
    return request('/api/admin/dashboard')
}

export function updateFeedbackModeration(id, status) {
    return request(`/api/admin/feedback/${id}/moderation`, {
        method: 'PATCH',
        body: JSON.stringify({ status }),
    })
}
