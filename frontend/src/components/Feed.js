import React, { useState, useEffect, useCallback } from 'react';
import { useAuth0 } from '@auth0/auth0-react';
import PostComposer from './PostComposer';
import PostCard from './PostCard';
import './Feed.css';

export default function Feed() {
    const { getAccessTokenSilently, isAuthenticated, loginWithRedirect } = useAuth0();
    const [posts, setPosts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);

    const fetchPosts = useCallback(async (pageNum = 0, append = false) => {
        try {
            const res = await fetch(
                `${process.env.REACT_APP_API_URL}/api/stream?page=${pageNum}&size=15`
            );
            const data = await res.json();
            if (data.length < 15) setHasMore(false);
            setPosts(prev => append ? [...prev, ...data] : data);
        } catch (err) {
            console.error('Failed to load posts:', err);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => { fetchPosts(0); }, [fetchPosts]);

    const handleNewPost = async (content) => {
        const token = await getAccessTokenSilently();
        const res = await fetch(`${process.env.REACT_APP_API_URL}/api/posts`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify({ content }),
        });
        if (!res.ok) {
            const text = await res.text().catch(() => '');
            console.error('POST /api/posts failed', res.status, text);
            throw new Error(`Failed to post (${res.status})`);
        }
        const newPost = await res.json();
        setPosts(prev => [newPost, ...prev]);
    };

    const loadMore = () => {
        const nextPage = page + 1;
        setPage(nextPage);
        fetchPosts(nextPage, true);
    };

    return (
        <div className="feed">
            {isAuthenticated ? (
                <PostComposer onPost={handleNewPost} />
            ) : (
                <div className="feed-empty">
                    <p>You are viewing the public stream.</p>
                    <button className="load-more" onClick={() => loginWithRedirect()}>
                        Sign in to create a post
                    </button>
                </div>
            )}

            <div className="feed-divider">
                <span className="divider-label">Public Stream</span>
            </div>

            {loading ? (
                <div className="feed-loading">
                    {[...Array(3)].map((_, i) => (
                        <div key={i} className="skeleton-card">
                            <div className="skeleton-header" />
                            <div className="skeleton-body" />
                            <div className="skeleton-body short" />
                        </div>
                    ))}
                </div>
            ) : posts.length === 0 ? (
                <div className="feed-empty">
                    <span className="empty-icon">◌</span>
                    <p>Nothing here yet. Be the first to post.</p>
                </div>
            ) : (
                <>
                    {posts.map(post => <PostCard key={post.id} post={post} />)}
                    {hasMore && (
                        <button className="load-more" onClick={loadMore}>
                            Load more
                        </button>
                    )}
                </>
            )}
        </div>
    );
}