import { useEffect, useState } from 'react';
import { apiClient } from '../services/api';
import type { PackingResponse } from '../types';

interface HistoryProps {
  onSelectList: (list: PackingResponse) => void;
  refreshTrigger?: number;
}

export default function History({ onSelectList, refreshTrigger }: HistoryProps) {
  const [history, setHistory] = useState<PackingResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadHistory();
  }, [refreshTrigger]);

  const loadHistory = async () => {
    setLoading(true);
    setError(null);
    try {
      const lists = await apiClient.getRecentLists(10);
      setHistory(lists);
    } catch (err: any) {
      setError(err.message || 'Failed to load history');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="history">
        <h2>Recent Packing Lists</h2>
        <div className="loading">Loading history...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="history">
        <h2>Recent Packing Lists</h2>
        <div className="error-message">
          <p>{error}</p>
          <button onClick={loadHistory} className="btn-secondary">
            Try Again
          </button>
        </div>
      </div>
    );
  }

  if (history.length === 0) {
    return (
      <div className="history">
        <h2>Recent Packing Lists</h2>
        <div className="empty-state">
          <p>No previous packing lists found.</p>
          <p>Generate your first one to see it here!</p>
        </div>
      </div>
    );
  }

  return (
    <div className="history">
      <h2>Recent Packing Lists</h2>
      <div className="history-list">
        {history.map((list) => (
          <div
            key={list.id}
            className="history-item"
            onClick={() => onSelectList(list)}
          >
            <div className="history-item-header">
              <h3>{list.destination}</h3>
            </div>
            <div className="history-item-footer">
              <span className="item-count">
                {list.categories ? Object.values(list.categories).reduce((sum, items) => sum + (Array.isArray(items) ? items.length : 0), 0) : 0} items
              </span>
            </div>
          </div>
        ))}
      </div>
      <button onClick={loadHistory} className="btn-secondary refresh-btn">
        Refresh History
      </button>
    </div>
  );
}
