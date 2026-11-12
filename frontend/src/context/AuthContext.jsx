import { createContext, useContext, useEffect, useState } from 'react'
import {
    login as loginRequest,
    register as registerRequest,
    logout as logoutRequest,
    getAccessToken,
} from '../services/authService'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
    const [token, setToken] = useState(getAccessToken())
    const [user, setUser] = useState(null)

    useEffect(() => {
        if (!token) {
            setUser(null)
            return
        }

        const controller = new AbortController()

        async function loadCurrentUser() {
            try {
                const response = await fetch('/api/users/me', {
                    headers: { Authorization: `Bearer ${token}` },
                    signal: controller.signal,
                })

                if (!response.ok) {
                    throw new Error('Unable to load account details')
                }

                const profile = await response.json()
                setUser(profile)
            } catch (error) {
                if (error.name !== 'AbortError') {
                    setUser(null)
                }
            }
        }

        loadCurrentUser()
        return () => controller.abort()
    }, [token])

    async function login(email, password) {
        const data = await loginRequest(email, password)
        setToken(data.accessToken)
        return data
    }

    async function register(email, password, displayName) {
        const data = await registerRequest(email, password, displayName)
        setToken(data.accessToken)
        return data
    }

    function logout() {
        logoutRequest()
        setToken(null)
        setUser(null)
    }

    return (
        <AuthContext.Provider
            value={{
                token,
                user,
                role: user?.role ?? null,
                isAdmin: user?.role === 'ADMIN',
                isAuthenticated: Boolean(token),
                login,
                register,
                logout,
            }}
        >
            {children}
        </AuthContext.Provider>
    )
}

export function useAuth() {
    return useContext(AuthContext)
}
