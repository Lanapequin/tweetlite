import React from 'react';
import { useAuth0 } from '@auth0/auth0-react';
import './Header.css';

export default function Header() {
    const { isAuthenticated, loginWithRedirect, logout, user } = useAuth0();

    return (
        <header className="header">
            <div className="header-inner">
                <div className="header-brand">
                    <span className="brand-icon">◈</span>
                    <span className="brand-name">TweetLite</span>
                </div>

                <nav className="header-nav">
                    {isAuthenticated ? (
                        <div className="user-area">
                            {user?.picture && (
                                <img
                                    src={user.picture}
                                    alt={user.name}
                                    className="user-avatar"
                                />
                            )}
                            <span className="user-name">{user?.name?.split(' ')[0]}</span>
                            <button
                                className="btn-logout"
                                onClick={() => logout({ logoutParams: { returnTo: window.location.origin } })}
                            >
                                Sign out
                            </button>
                        </div>
                    ) : (
                        <button className="btn-login" onClick={() => loginWithRedirect()}>
                            Sign in
                        </button>
                    )}
                </nav>
            </div>
        </header>
    );
}