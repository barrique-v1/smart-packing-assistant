import type { PackingResponse } from '../types';

interface PackingListProps {
  packingList: PackingResponse | null;
}

export default function PackingList({ packingList }: PackingListProps) {
  console.log('PackingList received:', packingList);

  if (!packingList) {
    return (
      <div className="empty-state">
        <h2>No Packing List Yet</h2>
        <p>Fill out the form to generate your personalized packing list.</p>
      </div>
    );
  }

  // Get categories from the response with safety checks
  console.log('Categories:', packingList.categories);
  const categoriesMap = packingList.categories || {};
  console.log('Categories map:', categoriesMap);

  const categories = Object.entries(categoriesMap)
    .filter(([, items]) => items && Array.isArray(items) && items.length > 0)
    .map(([category]) => category)
    .sort();

  console.log('Filtered categories:', categories);

  // Calculate total items with safety checks
  const totalItems = Object.values(categoriesMap).reduce((sum, items) => {
    return sum + (Array.isArray(items) ? items.length : 0);
  }, 0);

  console.log('Total items:', totalItems);

  return (
    <div className="packing-list">
      <div className="list-header">
        <h2>Packing List for {packingList.destination}</h2>
      </div>

      {/* Weather Info */}
      {packingList.weatherInfo && (
        <div className="info-section weather-info">
          <h3>🌤️ Weather Information</h3>
          <div className="weather-details">
            <p>
              <strong>Temperature Range:</strong> {packingList.weatherInfo.tempMin}°C - {packingList.weatherInfo.tempMax}°C
            </p>
            <p>
              <strong>Conditions:</strong> {packingList.weatherInfo.conditions}
            </p>
          </div>
        </div>
      )}

      {/* Culture Tips */}
      {packingList.cultureTips && packingList.cultureTips.length > 0 && (
        <div className="info-section culture-tips">
          <h3>🌍 Cultural Tips & Local Customs</h3>
          <ul className="culture-tips-list">
            {packingList.cultureTips.map((tip, index) => {
              // Parse tip format: "CATEGORY: Tip text"
              const [category, ...textParts] = tip.split(':');
              const text = textParts.join(':').trim();

              return (
                <li key={index} className="culture-tip-item">
                  <span className="tip-category">{category}</span>
                  <span className="tip-text">{text}</span>
                </li>
              );
            })}
          </ul>
        </div>
      )}

      {/* Items by Category */}
      <div className="categories">
        {categories.map((category) => (
          <div key={category} className="category-section">
            <h3 className="category-title">{category.charAt(0).toUpperCase() + category.slice(1)}</h3>
            <ul className="items-list">
              {categoriesMap[category as keyof typeof categoriesMap]?.map((item, index) => (
                <li key={index} className="item">
                  <div className="item-content">
                    <span className="item-name">{item.item}</span>
                    {item.reason && <span className="item-reason">{item.reason}</span>}
                  </div>
                  <span className="item-quantity">× {item.quantity}</span>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>

      <div className="list-footer">
        <p className="total-items">
          Total items: {totalItems}
        </p>
      </div>
    </div>
  );
}
