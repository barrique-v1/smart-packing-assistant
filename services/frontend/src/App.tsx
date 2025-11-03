import { useState, useEffect } from 'react';
import './App.css';
import PackingForm from './components/PackingForm';
import PackingList from './components/PackingList';
import History from './components/History';
import { apiClient } from './services/api';
import type { PackingRequest, PackingResponse } from './types';

type Tab = 'form' | 'results' | 'history';

function App() {
  const [activeTab, setActiveTab] = useState<Tab>('form');
  const [currentList, setCurrentList] = useState<PackingResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [apiHealthy, setApiHealthy] = useState<boolean | null>(null);
  const [historyRefreshTrigger, setHistoryRefreshTrigger] = useState(0);

  useEffect(() => {
    checkApiHealth();
  }, []);

  const checkApiHealth = async () => {
    const healthy = await apiClient.healthCheck();
    setApiHealthy(healthy);
  };

  const handleGenerateList = async (request: PackingRequest) => {
    setLoading(true);
    setError(null);

    try {
      console.log('Sending request:', request);
      const response = await apiClient.generatePackingList(request);
      console.log('Received response:', response);
      console.log('Response categories:', response?.categories);
      setCurrentList(response);
      setActiveTab('results');
      setHistoryRefreshTrigger((prev) => prev + 1);
    } catch (err: any) {
      console.error('Error generating packing list:', err);
      setError(err.message || 'Failed to generate packing list');
      setActiveTab('results');
    } finally {
      setLoading(false);
    }
  };

  const handleSelectFromHistory = (list: PackingResponse) => {
    setCurrentList(list);
    setActiveTab('results');
  };

  const handleNewList = () => {
    setCurrentList(null);
    setError(null);
    setActiveTab('form');
  };

  const handleRefreshSession = async () => {
    try {
      await apiClient.createSession();
      setError(null);
      alert('New session created successfully!');
    } catch (err: any) {
      setError('Failed to create new session: ' + err.message);
    }
  };

  return (
    <div className="app">
      <header className="app-header">
        <h1>Smart Packing Assistant</h1>
        <p className="tagline">AI-Powered Travel Packing Lists</p>
        {apiHealthy === false && (
          <div className="api-warning">
            Warning: API is not responding. Please ensure the backend is running on port 8080.
          </div>
        )}
      </header>

      <nav className="tabs">
        <button
          className={`tab ${activeTab === 'form' ? 'active' : ''}`}
          onClick={() => setActiveTab('form')}
        >
          New List
        </button>
        <button
          className={`tab ${activeTab === 'results' ? 'active' : ''}`}
          onClick={() => setActiveTab('results')}
          disabled={!currentList && !error}
        >
          Results
        </button>
        <button
          className={`tab ${activeTab === 'history' ? 'active' : ''}`}
          onClick={() => setActiveTab('history')}
        >
          History
        </button>
      </nav>

      <main className="content">
        {activeTab === 'form' && (
          <div className="tab-content">
            <PackingForm onSubmit={handleGenerateList} loading={loading} />
          </div>
        )}

        {activeTab === 'results' && (
          <div className="tab-content">
            {loading && (
              <div className="loading-state">
                <div className="spinner"></div>
                <p>Generating your personalized packing list...</p>
                <p className="loading-hint">This may take a few seconds.</p>
              </div>
            )}

            {!loading && error && (
              <div className="error-state">
                <h2>Oops! Something went wrong</h2>
                <div className="error-message">
                  <p>{error}</p>
                </div>
                <div className="error-actions">
                  <button onClick={handleNewList} className="btn-primary">
                    Try Again
                  </button>
                  {error.toLowerCase().includes('session') && (
                    <button onClick={handleRefreshSession} className="btn-secondary">
                      Refresh Session
                    </button>
                  )}
                </div>
              </div>
            )}

            {!loading && !error && currentList && (
              <>
                <PackingList packingList={currentList} />
                <div className="actions">
                  <button onClick={handleNewList} className="btn-primary">
                    Generate New List
                  </button>
                </div>
              </>
            )}

            {!loading && !error && !currentList && (
              <div className="empty-state">
                <h2>No Results Yet</h2>
                <p>Generate a packing list to see results here.</p>
                <button onClick={() => setActiveTab('form')} className="btn-primary">
                  Go to Form
                </button>
              </div>
            )}
          </div>
        )}

        {activeTab === 'history' && (
          <div className="tab-content">
            <History
              onSelectList={handleSelectFromHistory}
              refreshTrigger={historyRefreshTrigger}
            />
          </div>
        )}
      </main>

      <footer className="app-footer">
        <p>
          Smart Packing Assistant by Marcel Ratsch & Norman Peters
        </p>
      </footer>
    </div>
  );
}

export default App;
