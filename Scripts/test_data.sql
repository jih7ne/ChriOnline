-- =============================================
-- Test Data for ChriOnline E-Commerce
-- =============================================

-- 1. Insert Categories
INSERT INTO Categorie (nom, description) VALUES
('Électronique', 'Smartphones, ordinateurs, accessoires et gadgets électroniques.'),
('Vêtements', 'Vêtements pour hommes, femmes et enfants, mode et tendances.'),
('Maison & Décoration', 'Meubles, décoration d''intérieur et équipements pour la maison.');

-- 2. Insert Products (Produits)
-- Note: url_image uses https:// URLs because the JavaFX client uses new Image(url_image). 
-- This allows images to be loaded directly from the web.
INSERT INTO Produit (nom, description, prix, stock, url_image, id_categorie) VALUES
('Smartphone Samsung Galaxy S23', 'Smartphone 5G avec écran AMOLED 6.1 pouces et 128 Go de stockage.', 8500.00, 15, 'https://images.unsplash.com/photo-1610945415295-d9bbf067e59c?ixlib=rb-4.0.3&auto=format&fit=crop&w=600&q=80', 1),
('PC Portable Dell XPS 15', 'Ordinateur portable performant avec processeur i7, 16 Go de RAM et 512 Go SSD.', 15000.00, 5, 'https://images.unsplash.com/photo-1593642632823-8f785ba67e45?ixlib=rb-4.0.3&auto=format&fit=crop&w=600&q=80', 1),
('Casque Audio Sony WH-1000XM4', 'Casque sans fil à réduction de bruit active.', 3500.00, 20, 'https://images.unsplash.com/photo-1618366712010-f4ae9c647dcb?ixlib=rb-4.0.3&auto=format&fit=crop&w=600&q=80', 1),
('Veste en Cuir Homme', 'Veste en cuir véritable, style motard, couleur noire.', 1200.00, 10, 'https://images.unsplash.com/photo-1551028719-00167b16eac5?ixlib=rb-4.0.3&auto=format&fit=crop&w=600&q=80', 2),
('Robe d''été en Lin', 'Robe légère en lin pour femme, parfaite pour les journées ensoleillées.', 450.00, 30, 'https://images.unsplash.com/photo-1595777457583-95e059d581b8?ixlib=rb-4.0.3&auto=format&fit=crop&w=600&q=80', 2),
('Canapé 3 Places Scandinave', 'Canapé confortable avec revêtement en tissu gris et pieds en bois massif.', 4500.00, 3, 'https://images.unsplash.com/photo-1493663284031-b7e3aefcae8e?ixlib=rb-4.0.3&auto=format&fit=crop&w=600&q=80', 3),
('Lampe de Bureau LED', 'Lampe de bureau design avec luminosité réglable et port USB.', 250.00, 50, 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?ixlib=rb-4.0.3&auto=format&fit=crop&w=600&q=80', 3);
