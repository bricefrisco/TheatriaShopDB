package com.playtheatria.shopdb.services;

import com.playtheatria.shopdb.database.ChestShop;
import com.playtheatria.shopdb.database.Player;
import com.playtheatria.shopdb.database.Region;
import com.playtheatria.shopdb.models.chestshops.EventType;
import com.playtheatria.shopdb.models.chestshops.Location;
import com.playtheatria.shopdb.models.chestshops.Server;
import com.playtheatria.shopdb.models.chestshops.ShopEvent;
import com.playtheatria.shopdb.models.regions.RegionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.*;

@ApplicationScoped
public class ChestShopService {
    final Logger LOGGER = LoggerFactory.getLogger(ChestShopService.class);
    final RegionService regionService;
    private static final String RESPONSE = "Successfully inserted/updated %s player(s), %s region(s), and %s chest shop(s). Removed %s chest shop(s).";

    public ChestShopService(RegionService regionService) {
        this.regionService = regionService;
    }

    public String createChestShopSigns(List<ShopEvent> shopEvents) {
        List<String> shopIdsToDelete = new ArrayList<>();
        List<ShopEvent> upserts = new ArrayList<>();
        Set<String> playerNames = new HashSet<>();

        LOGGER.info("Sorting shop events...");
        for (ShopEvent shopEvent : shopEvents) {
            if (shopEvent.getEventType().equals(EventType.DELETE)) {
                shopIdsToDelete.add(shopEvent.getId());
            } else {
                upserts.add(shopEvent);
                playerNames.add(shopEvent.getOwner().toLowerCase(Locale.ROOT));
                for (RegionRequest regionRequest : shopEvent.getRegions()) {
                    playerNames.addAll(regionRequest.getMayorNames());
                }
            }
        }

        LOGGER.info("Found " + shopIdsToDelete.size() + " shop deletion events.");
        LOGGER.info("Found " + upserts.size() + " shops to create or modify.");

        LOGGER.info("Deleting " + shopIdsToDelete.size() + " shop events...");
        for (String id : shopIdsToDelete) {
            ChestShop.deleteById(id);
        }

        if (upserts.size() == 0) {
            String response = String.format(RESPONSE, 0, 0, 0, shopIdsToDelete.size());
            LOGGER.info(response);
            return response;
        }

        LOGGER.info("Retrieving/adding " + playerNames.size() + " players...");
        HashMap<String, Player> players = Player.getOrAddPlayers(playerNames);

        Set<RegionRequest> regionRequests = new HashSet<>();
        for (ShopEvent e : upserts) {
            regionRequests.addAll(e.getRegions());
        }
        LOGGER.info("Inserting/updating " + regionRequests.size() + " regions...");
        HashMap<String, Region> regions = regionService.upsertRegions(regionRequests, players);


        LOGGER.info("Mapping " + shopEvents.size() + " events to chest shops...");
        List<ChestShop> chestShops = new ArrayList<>();
        for (ShopEvent upsert : upserts) {
            if (!eventIsValid(upsert)) continue;
            Optional<ChestShop> maybeChestShop = ChestShop.findByIdOptional(upsert.getId());
            ChestShop chestShop = maybeChestShop.map(shop -> convert(shop, upsert, players, regions)).orElseGet(() -> convert(upsert, players, regions));
            chestShops.add(chestShop);
        }

        LOGGER.info("Adding " + chestShops.size() + " chest shops...");
        if (chestShops.size() > 0) {
            ChestShop.persist(chestShops);
        }

        String response = String.format(RESPONSE, players.keySet().size(), regions.keySet().size(), upserts.size(), shopIdsToDelete.size());
        LOGGER.info(response);
        return response;
    }

    public void linkAndShowChestShops(Region region) {
        List<ChestShop> shops = ChestShop.findInRegion(region);
        for (ChestShop shop : shops) {
            shop.town = region;
            shop.isHidden = Boolean.FALSE;
        }
        ChestShop.persist(shops);
    }

    public void linkAndHideChestShops(Region region) {
        List<ChestShop> shops = ChestShop.findInRegion(region);
        for (ChestShop shop : shops) {
            shop.town = region;
            shop.isHidden = Boolean.TRUE;
        }
        ChestShop.persist(shops);
    }

    private boolean eventIsValid(ShopEvent event) {
        if (event.getId() == null || event.getId().isEmpty()) {
            LOGGER.info("Skipping event " + event + " - ID is null or empty.");
            return false;
        }

        if (event.getEventType() == null) {
            LOGGER.info("Skipping event " + event + " - event type not specified.");
            return false;
        }

        if (event.getWorld() == null || event.getWorld().isEmpty()) {
            LOGGER.info("Skipping event " + event + " - no world specified.");
            return false;
        }

        if (!event.getWorld().equals("The_Ark")) {
            LOGGER.info("Skipping event " + event.toString() + " - server cannot be determined.");
            return false;
        }

        if (event.getX() == null || event.getY() == null || event.getZ() == null) {
            LOGGER.info("Skipping event " + event + " - X, Y, or Z coordinate is missing.");
            return false;
        }

        if (event.getOwner() == null || event.getOwner().isEmpty()) {
            LOGGER.info("Skipping event " + event + " - owner is missing");
            return false;
        }

        if (event.getQuantity() == null || event.getQuantity() == 0) {
            LOGGER.info("Skipping event " + event + " - shop quantity is missing");
            return false;
        }

        if (event.getCount() == null) {
            LOGGER.info("Skipping event " + event + " - count is missing");
            return false;
        }

        if (event.getItem() == null || event.getItem().isEmpty()) {
            LOGGER.info("Skipping event " + event + " - item is missing");
            return false;
        }

        if (event.getFull() == null) {
            LOGGER.info("Skipping event " + event + " - 'full' indicator is missing");
            return false;
        }

        return true;
    }

    private ChestShop convert(ShopEvent event, HashMap<String, Player> players, HashMap<String, Region> regions) {
        ChestShop chestShop = new ChestShop();
        chestShop.id = event.getId();

        String world = event.getWorld();
        if ("The_Ark".equals(world)) {
            chestShop.server = Server.THE_ARK;
        }

        Location location = new Location();
        location.setX(event.getX());
        location.setY(event.getY());
        location.setZ(event.getZ());
        chestShop.location = location;

        return convert(chestShop, event, players, regions);
    }

    private ChestShop convert(ChestShop sign, ShopEvent event, HashMap<String, Player> players, HashMap<String, Region> regions) {
        sign.owner = players.get(event.getOwner().toLowerCase(Locale.ROOT));
        sign.quantity = event.getQuantity();
        sign.quantityAvailable = event.getCount();

        if (event.getSellPrice() != null && event.getSellPrice().doubleValue() != -1.0) {
            sign.sellPrice = event.getSellPrice().doubleValue();
            sign.sellPriceEach = determineSellPriceEach(event.getQuantity(), event.getSellPrice().doubleValue());
            sign.isSellSign = Boolean.TRUE;
        } else {
            sign.sellPrice = null;
            sign.sellPriceEach = null;
            sign.isSellSign = Boolean.FALSE;
        }

        if (event.getBuyPrice() != null && event.getBuyPrice().doubleValue() != -1.0) {
            sign.buyPrice = event.getBuyPrice().doubleValue();
            sign.buyPriceEach = determineBuyPriceEach(event.getQuantity(), event.getBuyPrice().doubleValue());
            sign.isBuySign = Boolean.TRUE;
        } else {
            sign.buyPrice = null;
            sign.buyPriceEach = null;
            sign.isBuySign = Boolean.FALSE;
        }

        List<Region> shopRegions = new ArrayList<>();
        for (RegionRequest regionReq : event.getRegions()) {
            Region r = regions.get(regionReq.getName() + "|" + regionReq.getServer());
            if (r != null) shopRegions.add(r);
        }

        sign.town = regionService.findActiveOrSmallest(shopRegions);
        sign.material = event.getItem().toLowerCase(Locale.ROOT);
        sign.isHidden = sign.town == null || !sign.town.active;
        sign.isFull = event.getFull();
        sign.isSellSign = sign.sellPrice != null;
        return sign;
    }

    private Double determineSellPriceEach(Integer quantity, Double sellPrice) {
        return quantity == null || sellPrice == null ? null : sellPrice / quantity;
    }

    private Double determineBuyPriceEach(Integer quantity, Double buyPrice) {
        return quantity == null || buyPrice == null ? null : buyPrice / quantity;
    }
}
