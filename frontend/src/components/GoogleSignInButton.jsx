import { useEffect, useRef, useState } from 'react'

const GOOGLE_SCRIPT = 'https://accounts.google.com/gsi/client'

export default function GoogleSignInButton({ onCredential, disabled = false, onError }) {
    const buttonRef = useRef(null)
    const [ready, setReady] = useState(false)
    const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID

    useEffect(() => {
        if (!clientId) return undefined

        let cancelled = false
        const initialize = () => {
            if (cancelled || !window.google?.accounts?.id || !buttonRef.current) return
            window.google.accounts.id.initialize({
                client_id: clientId,
                callback: (response) => onCredential(response.credential),
                auto_select: false,
                cancel_on_tap_outside: true,
            })
            buttonRef.current.innerHTML = ''
            window.google.accounts.id.renderButton(buttonRef.current, {
                type: 'standard',
                theme: 'outline',
                size: 'large',
                shape: 'rectangular',
                text: 'continue_with',
                logo_alignment: 'left',
                width: Math.min(360, buttonRef.current.parentElement?.clientWidth || 360),
            })
            setReady(true)
        }

        const existing = document.querySelector(`script[src="${GOOGLE_SCRIPT}"]`)
        if (window.google?.accounts?.id) {
            initialize()
        } else if (existing) {
            existing.addEventListener('load', initialize, { once: true })
        } else {
            const script = document.createElement('script')
            script.src = GOOGLE_SCRIPT
            script.async = true
            script.defer = true
            script.onload = initialize
            script.onerror = () => onError?.('Could not load Google sign-in. Check your connection.')
            document.head.appendChild(script)
        }

        return () => {
            cancelled = true
        }
    }, [clientId, onCredential, onError])

    if (!clientId) {
        return <p className="google-config-note">Google sign-in will be available after OAuth setup.</p>
    }

    return (
        <div className={`google-signin-wrap ${disabled || !ready ? 'is-loading' : ''}`}>
            <div ref={buttonRef} />
        </div>
    )
}
