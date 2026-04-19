import React from 'react';
import { formatDistanceToNow } from 'date-fns';
import './PostCard.css';

export default function PostCard({ post }) {
    const timeAgo = formatDistanceToNow(new Date(post.createdAt), { addSuffix: true });
    const initials = (post.authorName || 'A')
        .split(' ')
        .map(n => n[0])
        .join('')
        .toUpperCase()
        .slice(0, 2);

    return (
        <article className="post-card">
            <div className="post-author-col">
                {post.authorPicture ? (
                    <img src={post.authorPicture} alt="" className="post-avatar" />
                ) : (
                    <div className="post-avatar-placeholder">{initials}</div>
                )}
                <div className="post-thread-line" />
            </div>
            <div className="post-body">
                <div className="post-header">
                    <span className="post-name">{post.authorName || 'Anonymous'}</span>
                    <span className="post-time">{timeAgo}</span>
                </div>
                <p className="post-content">{post.content}</p>
            </div>
        </article>
    );
}