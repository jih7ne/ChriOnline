package com.chrionline.chrionline.server.services;

import com.chrionline.chrionline.server.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminService {
    private final ProduitRepository produitRepository;
    private final CommandeRepository commandeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PaiementRepository paiementRepository;
    private final CategorieRepository categorieRepository;

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);


    public AdminService(ProduitRepository produitRepository, CommandeRepository commandeRepository, UtilisateurRepository utilisateurRepository, PaiementRepository paiementRepository, CategorieRepository categorieRepository) {
        this.produitRepository = produitRepository;
        this.commandeRepository = commandeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.paiementRepository = paiementRepository;
        this.categorieRepository = categorieRepository;
    }






}
