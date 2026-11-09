
import { createContext, useContext, useState } from 'react'
import {
    login as loginRequest,
    register as registerRequest,
    logout as logoutRequest,
    getAccessToken,
} from '../services/authService'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
    const [token, setToken] = useState(getAccessToken())

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
    }

    return (
        <AuthContext.Provider
            value={{
                token,
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
