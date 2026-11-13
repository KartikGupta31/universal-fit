async function apiRequest(url, options = {}) {
    const response = await fetch(url, options)

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

function saveTokens(data) {
    if (!data.accessToken || !data.refreshToken) {
        throw new Error('Authentication response did not contain tokens.')
    }

    sessionStorage.setItem('accessToken', data.accessToken)
    sessionStorage.setItem('refreshToken', data.refreshToken)
}

export async function login(email, password) {
    const data = await apiRequest('/auth/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password }),
    })

    saveTokens(data)
    return data
}

export async function register(email, password, displayName) {
    const data = await apiRequest('/auth/register', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password, displayName }),
    })

    saveTokens(data)
    return data
}

// Exchange the refresh token for a new access/refresh token pair.
// The backend rotates refresh tokens, so both values must be stored.
export async function refreshAccessToken() {
    const refreshToken = sessionStorage.getItem('refreshToken')
    if (!refreshToken) return null

    try {
        const data = await apiRequest('/auth/refresh', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ refreshToken }),
        })

        saveTokens(data)
        return data.accessToken
    } catch {
        logout()
        return null
    }
}

export function logout() {
    sessionStorage.removeItem('accessToken')
    sessionStorage.removeItem('refreshToken')
}

export function getAccessToken() {
    return sessionStorage.getItem('accessToken')
}
