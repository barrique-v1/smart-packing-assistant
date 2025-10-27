import { useState } from 'react';
import type { FormEvent } from 'react';
import type { PackingRequest, TravelType, Season } from '../types';

interface PackingFormProps {
  onSubmit: (request: PackingRequest) => void;
  loading: boolean;
}

const TRAVEL_TYPES: { value: TravelType; label: string }[] = [
  { value: 'VACATION', label: 'Vacation' },
  { value: 'BUSINESS', label: 'Business' },
  { value: 'BACKPACKING', label: 'Backpacking' },
];

const SEASONS: { value: Season; label: string }[] = [
  { value: 'SPRING', label: 'Spring' },
  { value: 'SUMMER', label: 'Summer' },
  { value: 'FALL', label: 'Fall' },
  { value: 'WINTER', label: 'Winter' },
];

export default function PackingForm({ onSubmit, loading }: PackingFormProps) {
  const [destination, setDestination] = useState('');
  const [durationDays, setDurationDays] = useState(7);
  const [travelType, setTravelType] = useState<TravelType>('VACATION');
  const [season, setSeason] = useState<Season>('SUMMER');

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();

    if (!destination.trim()) {
      alert('Please enter a destination');
      return;
    }

    if (durationDays < 1 || durationDays > 365) {
      alert('Duration must be between 1 and 365 days');
      return;
    }

    onSubmit({
      destination: destination.trim(),
      durationDays,
      travelType,
      season,
    });
  };

  return (
    <form onSubmit={handleSubmit} className="packing-form">
      <div className="form-group">
        <label htmlFor="destination">
          Destination <span className="required">*</span>
        </label>
        <input
          type="text"
          id="destination"
          value={destination}
          onChange={(e) => setDestination(e.target.value)}
          placeholder="e.g., Paris, Tokyo, New York"
          disabled={loading}
          required
        />
      </div>

      <div className="form-group">
        <label htmlFor="duration">
          Duration (days) <span className="required">*</span>
        </label>
        <input
          type="number"
          id="duration"
          value={durationDays}
          onChange={(e) => setDurationDays(parseInt(e.target.value) || 1)}
          min="1"
          max="365"
          disabled={loading}
          required
        />
      </div>

      <div className="form-group">
        <label htmlFor="season">
          Season <span className="required">*</span>
        </label>
        <select
          id="season"
          value={season}
          onChange={(e) => setSeason(e.target.value as Season)}
          disabled={loading}
          required
        >
          {SEASONS.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>
      </div>

      <div className="form-group">
        <label htmlFor="travelType">
          Travel Type <span className="required">*</span>
        </label>
        <select
          id="travelType"
          value={travelType}
          onChange={(e) => setTravelType(e.target.value as TravelType)}
          disabled={loading}
          required
        >
          {TRAVEL_TYPES.map((t) => (
            <option key={t.value} value={t.value}>
              {t.label}
            </option>
          ))}
        </select>
      </div>

      <button type="submit" className="btn-primary" disabled={loading}>
        {loading ? 'Generating...' : 'Generate Packing List'}
      </button>
    </form>
  );
}
