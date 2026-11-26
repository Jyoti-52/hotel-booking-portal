-- Sample data: 7 hotels. Room counts are deliberately small so that
-- "sold out" and the date-suggestion feature are easy to demonstrate.

INSERT INTO hotels (name, location, description, amenities, price_per_night, total_rooms) VALUES
('Palm Grove Beach Resort', 'Goa',
 'Beachfront resort a short walk from Calangute beach, with a large pool and open-air restaurant.',
 'WiFi,Pool,Breakfast,Beach Access,Air Conditioning', 6500.00, 6),

('Heritage Haveli Jaipur', 'Jaipur',
 'Restored 19th-century haveli in the old city with courtyard dining and rooftop views of Nahargarh Fort.',
 'WiFi,Breakfast,Rooftop Restaurant,Air Conditioning', 4800.00, 4),

('Marine Drive Grand', 'Mumbai',
 'Business hotel facing the Arabian Sea, ten minutes from Churchgate station.',
 'WiFi,Gym,Breakfast,Airport Shuttle,Air Conditioning', 9200.00, 8),

('Garden City Suites', 'Bengaluru',
 'Modern suites near the tech parks of Whitefield with a work lounge and fast WiFi.',
 'WiFi,Gym,Work Lounge,Parking,Air Conditioning', 5400.00, 5),

('Backwater Bliss Retreat', 'Kochi',
 'Quiet retreat on the Kochi backwaters with houseboat excursions and Kerala cuisine.',
 'WiFi,Breakfast,Boat Rides,Ayurvedic Spa', 5900.00, 3),

('Capital Comfort Inn', 'Delhi',
 'Budget-friendly hotel five minutes from Connaught Place and the metro.',
 'WiFi,Breakfast,Air Conditioning,24h Reception', 3200.00, 10),

('Lakeview Palace Boutique', 'Udaipur',
 'Small boutique hotel on Lake Pichola. Only two rooms, so it sells out quickly.',
 'WiFi,Breakfast,Lake View,Candle-lit Dinners', 12500.00, 2);
