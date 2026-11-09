
const API_URL = '/api'

async function measurementRequest(endpoint, options = {}) {
    const token = sessionStorage.getItem('accessToken')

    const response = await fetch(`${API_URL}${endpoint}`, {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
            ...options.headers,
        },
    })

    if (response.status === 204) {
        return null
    }

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

// Get logged-in user's details and consent status
export async function getCurrentUser() {
    return measurementRequest('/users/me')
}

// Grant or revoke measurement consent
export async function updateMeasurementConsent(consent) {
    return measurementRequest('/measurements/consent', {
        method: 'PUT',
        body: JSON.stringify({ consent }),
    })
}

// Get saved measurement profile
export async function getMeasurements() {
    return measurementRequest('/measurements/me')
}

// Save or update measurement profile
export async function saveMeasurements(profile) {
    return measurementRequest('/measurements/me', {
        method: 'PUT',
        body: JSON.stringify(profile),
    })
}

// Get measurement history
export async function getMeasurementHistory(page = 0, size = 10) {
    return measurementRequest(
        `/measurements/history?page=${page}&size=${size}`
    )
}

// Delete measurement profile
export async function deleteMeasurements() {
    return measurementRequest('/measurements/me', {
        method: 'DELETE',
    })
}
