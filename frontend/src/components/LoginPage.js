import React from 'react';
import { useAuth0 } from '@auth0/auth0-react';
import './LoginPage.css';

export default function LoginPage() {
    const { loginWithRedirect } = useAuth0();

    return (
        <div className="login-page">
            <div className="login-hero">
                <div className="login-glyph">◈</div>
                <h1 className="login-title">TweetLite</h1>
                <p className="login-sub">A minimal public stream. Say something.</p>

                <div className="login-features">
                    <div className="feature">
                        <span className="feature-dot" />
                        <span>140 characters max</span>
                    </div>
                    <div className="feature">
                        <span className="feature-dot" />
                        <span>One public stream for everyone</span>
                    </div>
                    <div className="feature">
                        <span className="feature-dot" />
                        <span>Secured with Auth0</span>
                    </div>
                </div>

                <button className="login-cta" onClick={() => loginWithRedirect()}>
                    Get started — it's free
                </button>

                <p className="login-note">Continue as guest to read the stream ↓</p>
            </div>

            <PublicStreamPreview />
        </div>
    );
}

function PublicStreamPreview() {
    const [posts, setPosts] = React.useState([]);

    React.useEffect(() => {
        fetch(`${process.env.REACT_APP_API_URL}/api/stream?size=3`)
            .then(r => r.json())
            .then(setPosts)
            .catch(() => {});
    }, []);

    if (!posts.length) return null;

    return (
        <div className="stream-preview">
            <p className="preview-label">Recent posts</p>
            {posts.map(p => (
                <div key={p.id} className="preview-card">
                    <span className="preview-author">{p.authorName || 'Anonymous'}</span>
                    <span className="preview-content">{p.content}</span>
                </div>
            ))}
        </div>
    );
}