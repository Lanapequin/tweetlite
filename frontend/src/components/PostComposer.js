import React, { useState } from 'react';
import { useAuth0 } from '@auth0/auth0-react';
import './PostComposer.css';

export default function PostComposer({ onPost }) {
    const { user } = useAuth0();
    const [content, setContent] = useState('');
    const [posting, setPosting] = useState(false);
    const [error, setError] = useState('');

    const remaining = 140 - content.length;
    const isValid = content.trim().length > 0 && remaining >= 0;

    const handleSubmit = async () => {
        if (!isValid || posting) return;
        setPosting(true);
        setError('');
        try {
            await onPost(content.trim());
            setContent('');
        } catch (e) {
            setError('Failed to post. Try again.');
        } finally {
            setPosting(false);
        }
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) handleSubmit();
    };

    return (
        <div className="composer">
            <div className="composer-top">
                {user?.picture && (
                    <img src={user.picture} alt="" className="composer-avatar" />
                )}
                <div className="composer-right">
          <textarea
              className="composer-textarea"
              placeholder="What's on your mind?"
              value={content}
              onChange={e => setContent(e.target.value)}
              onKeyDown={handleKeyDown}
              maxLength={145}
              rows={3}
          />
                    <div className="composer-footer">
                        <div className="composer-meta">
              <span className={`char-count ${remaining < 20 ? 'warn' : ''} ${remaining < 0 ? 'over' : ''}`}>
                {remaining}
              </span>
                            {error && <span className="composer-error">{error}</span>}
                        </div>
                        <button
                            className="btn-post"
                            onClick={handleSubmit}
                            disabled={!isValid || posting}
                        >
                            {posting ? 'Posting…' : 'Post'}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}