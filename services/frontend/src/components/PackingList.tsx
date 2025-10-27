import type { PackingResponse, PackingItem } from '../types';

interface PackingListProps {
  packingList: PackingResponse | null;
}

export default function PackingList({ packingList }: PackingListProps) {
  if (!packingList) {
    return (
      <div className="empty-state">
        <h2>No Packing List Yet</h2>
        <p>Fill out the form to generate your personalized packing list.</p>
      </div>
    );
  }

  // Group items by category
  const itemsByCategory = packingList.items.reduce((acc, item) => {
    if (!acc[item.category]) {
      acc[item.category] = [];
    }
    acc[item.category].push(item);
    return acc;
  }, {} as Record<string, PackingItem[]>);

  const categories = Object.keys(itemsByCategory).sort();

  return (
    <div className="packing-list">
      <div className="list-header">
        <h2>Packing List for {packingList.destination}</h2>
        <div className="list-meta">
          <span className="badge">{packingList.durationDays} days</span>
          <span className="badge">{packingList.season}</span>
          <span className="badge">{packingList.travelType}</span>
        </div>
      </div>

      {/* Weather Info */}
      {packingList.weatherInfo && (
        <div className="info-section weather-info">
          <h3>Weather Information</h3>
          <p>
            <strong>Temperature:</strong> {packingList.weatherInfo.tempMin}°C -{' '}
            {packingList.weatherInfo.tempMax}°C
          </p>
          <p>
            <strong>Conditions:</strong> {packingList.weatherInfo.conditions}
          </p>
        </div>
      )}

      {/* Culture Tips */}
      {packingList.cultureTips && packingList.cultureTips.length > 0 && (
        <div className="info-section culture-tips">
          <h3>Culture Tips</h3>
          <ul>
            {packingList.cultureTips.map((tip, index) => (
              <li key={index}>{tip}</li>
            ))}
          </ul>
        </div>
      )}

      {/* Items by Category */}
      <div className="categories">
        {categories.map((category) => (
          <div key={category} className="category-section">
            <h3 className="category-title">{category}</h3>
            <ul className="items-list">
              {itemsByCategory[category].map((item, index) => (
                <li key={index} className="item">
                  <span className="item-name">{item.name}</span>
                  <span className="item-quantity">× {item.quantity}</span>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>

      <div className="list-footer">
        <p className="total-items">
          Total items: {packingList.items.length}
        </p>
        <p className="created-at">
          Created: {new Date(packingList.createdAt).toLocaleString()}
        </p>
      </div>
    </div>
  );
}
