import React from 'react';
import { useAuth0 } from '@auth0/auth0-react';
import Header from './components/Header';
import Feed from './components/Feed';
import LoginPage from './components/LoginPage';
import './App.css';

function App() {
  const { isLoading, isAuthenticated, error } = useAuth0();

  if (isLoading) {
    return (
        <div className="loading-screen">
          <div className="loading-logo">TweetLite</div>
          <div className="loading-spinner" />
        </div>
    );
  }

  if (error) {
    return (
        <div className="error-screen">
          <p>Auth error: {error.message}</p>
        </div>
    );
  }

  return (
      <div className="app">
        <Header />
        <main className="main-content">
          {isAuthenticated ? <Feed /> : <LoginPage />}
        </main>
      </div>
  );
}

export default App;