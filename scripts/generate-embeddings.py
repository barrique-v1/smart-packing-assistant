#!/usr/bin/env python3
"""
Generate embeddings for packing knowledge base and format for Qdrant import.

This script:
1. Reads packing items from CSV
2. Generates embeddings using OpenAI text-embedding-3-small
3. Formats data as Qdrant points
4. Saves to JSON file for import

Usage:
    export OPENAI_API_KEY=sk-your-key-here
    python3 scripts/generate-embeddings.py
"""

import csv
import json
import os
import sys
import uuid
from typing import List, Dict, Any
from openai import OpenAI
from tqdm import tqdm
import time

# Configuration
CSV_FILE = "data/packing-knowledge.csv"
OUTPUT_FILE = "data/packing-embeddings.json"
EMBEDDING_MODEL = "text-embedding-3-small"
EMBEDDING_DIMENSIONS = 1536
BATCH_SIZE = 100  # Process in batches for API rate limits
RETRY_ATTEMPTS = 3
RETRY_DELAY = 2  # seconds

def load_knowledge_base(csv_path: str) -> List[Dict[str, Any]]:
    """Load packing knowledge from CSV file."""
    items = []
    print(f"📖 Loading knowledge base from {csv_path}...")

    try:
        with open(csv_path, 'r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            for row in reader:
                # Parse comma-separated fields
                seasons = [s.strip() for s in row['season'].split(',')]
                tags = [t.strip() for t in row.get('tags', '').split(';') if t.strip()]
                climate = [c.strip() for c in row.get('climate', '').split(';') if c.strip()]

                items.append({
                    'item': row['item'],
                    'category': row['category'],
                    'destination_type': row['destination_type'],
                    'travel_type': row['travel_type'],
                    'season': seasons,
                    'quantity': int(row['quantity']),
                    'reason': row['reason'],
                    'importance': row['importance'],
                    'tags': tags,
                    'climate': climate
                })

        print(f"✅ Loaded {len(items)} items from knowledge base")
        return items

    except FileNotFoundError:
        print(f"❌ Error: File not found: {csv_path}")
        print(f"   Current directory: {os.getcwd()}")
        sys.exit(1)
    except Exception as e:
        print(f"❌ Error loading CSV: {e}")
        sys.exit(1)

def create_embedding_text(item: Dict[str, Any]) -> str:
    """Create rich text representation for embedding.

    This text is what OpenAI will convert to a vector.
    It includes all relevant information about the item.
    """
    return f"""Item: {item['item']}
Category: {item['category']}
Travel Type: {item['travel_type']}
Destination: {item['destination_type']}
Season: {', '.join(item['season'])}
Reason: {item['reason']}
Tags: {', '.join(item['tags'])}
Climate: {', '.join(item['climate'])}
Importance: {item['importance']}""".strip()

def generate_embeddings(items: List[Dict[str, Any]], api_key: str) -> List[Dict[str, Any]]:
    """Generate embeddings for all items using OpenAI API."""
    client = OpenAI(api_key=api_key)
    points = []

    print(f"\n🤖 Generating embeddings for {len(items)} items...")
    print(f"   Model: {EMBEDDING_MODEL} ({EMBEDDING_DIMENSIONS} dimensions)")
    print(f"   Batch size: {BATCH_SIZE}")

    # Process in batches
    for i in tqdm(range(0, len(items), BATCH_SIZE), desc="Processing batches"):
        batch = items[i:i + BATCH_SIZE]
        texts = [create_embedding_text(item) for item in batch]

        # Retry logic for API calls
        for attempt in range(RETRY_ATTEMPTS):
            try:
                response = client.embeddings.create(
                    model=EMBEDDING_MODEL,
                    input=texts,
                    dimensions=EMBEDDING_DIMENSIONS
                )

                # Format as Qdrant points
                for j, embedding_obj in enumerate(response.data):
                    item = batch[j]
                    point = {
                        'id': str(uuid.uuid4()),
                        'vector': embedding_obj.embedding,
                        'payload': {
                            'item': item['item'],
                            'category': item['category'],
                            'destination_type': item['destination_type'],
                            'travel_type': item['travel_type'],
                            'season': item['season'],
                            'quantity': item['quantity'],
                            'reason': item['reason'],
                            'importance': item['importance'],
                            'tags': item['tags'],
                            'climate': item['climate']
                        }
                    }
                    points.append(point)

                # Success - break retry loop
                break

            except Exception as e:
                if attempt < RETRY_ATTEMPTS - 1:
                    print(f"\n⚠️  API error (attempt {attempt + 1}/{RETRY_ATTEMPTS}): {e}")
                    print(f"   Retrying in {RETRY_DELAY} seconds...")
                    time.sleep(RETRY_DELAY)
                else:
                    print(f"\n❌ Failed after {RETRY_ATTEMPTS} attempts: {e}")
                    sys.exit(1)

        # Rate limiting delay
        if i + BATCH_SIZE < len(items):
            time.sleep(0.5)

    print(f"\n✅ Generated {len(points)} embeddings successfully")
    return points

def save_embeddings(points: List[Dict[str, Any]], output_path: str):
    """Save embeddings to JSON file for Qdrant import."""
    print(f"\n💾 Saving embeddings to {output_path}...")

    try:
        # Create output directory if it doesn't exist
        os.makedirs(os.path.dirname(output_path), exist_ok=True)

        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump({
                'points': points,
                'metadata': {
                    'total_items': len(points),
                    'embedding_model': EMBEDDING_MODEL,
                    'dimensions': EMBEDDING_DIMENSIONS,
                    'generated_at': time.strftime('%Y-%m-%d %H:%M:%S')
                }
            }, f, indent=2)

        # Calculate file size
        file_size = os.path.getsize(output_path) / (1024 * 1024)  # MB
        print(f"✅ Saved {len(points)} points to {output_path}")
        print(f"   File size: {file_size:.2f} MB")

    except Exception as e:
        print(f"❌ Error saving file: {e}")
        sys.exit(1)

def print_statistics(items: List[Dict[str, Any]]):
    """Print statistics about the knowledge base."""
    print("\n📊 Knowledge Base Statistics:")
    print(f"   Total items: {len(items)}")

    # Count by category
    categories = {}
    for item in items:
        cat = item['category']
        categories[cat] = categories.get(cat, 0) + 1

    print("   Items by category:")
    for cat, count in sorted(categories.items()):
        print(f"      - {cat}: {count}")

    # Count by travel type
    travel_types = {}
    for item in items:
        tt = item['travel_type']
        travel_types[tt] = travel_types.get(tt, 0) + 1

    print("   Items by travel type:")
    for tt, count in sorted(travel_types.items()):
        print(f"      - {tt}: {count}")

    # Count by importance
    importance = {}
    for item in items:
        imp = item['importance']
        importance[imp] = importance.get(imp, 0) + 1

    print("   Items by importance:")
    for imp, count in sorted(importance.items(),
                              key=lambda x: {'critical': 4, 'high': 3, 'medium': 2, 'low': 1}.get(x[0], 0),
                              reverse=True):
        print(f"      - {imp}: {count}")

def main():
    """Main execution function."""
    print("=" * 70)
    print("  Smart Packing Assistant - Embedding Generation")
    print("=" * 70)

    # Check for API key
    api_key = os.getenv('OPENAI_API_KEY')
    if not api_key:
        print("\n❌ Error: OPENAI_API_KEY environment variable not set")
        print("   Please set it with: export OPENAI_API_KEY=sk-your-key-here")
        sys.exit(1)

    print(f"✅ OpenAI API key found (starts with '{api_key[:10]}...')")

    # Load knowledge base
    items = load_knowledge_base(CSV_FILE)

    # Print statistics
    print_statistics(items)

    # Generate embeddings
    points = generate_embeddings(items, api_key)

    # Save to file
    save_embeddings(points, OUTPUT_FILE)

    print("\n" + "=" * 70)
    print("✅ Embedding generation complete!")
    print("=" * 70)
    print(f"\nNext steps:")
    print(f"1. Review the generated file: {OUTPUT_FILE}")
    print(f"2. Import to Qdrant using: python3 scripts/import-to-qdrant.py")
    print()

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n⚠️  Interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ Unexpected error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
