package com.playtheatria.shopdb.models.chestshops;

import com.playtheatria.shopdb.database.ChestShop;
import com.playtheatria.shopdb.database.Player;
import com.playtheatria.shopdb.database.Region;

public final class ChestShopMapper {
    public static ChestShopRegionDto toChestShopRegionDto(Region region) {
        if (region == null) {
            return null;
        }

        ChestShopRegionDto result = new ChestShopRegionDto();
        result.setName(region.getName());
        return result;
    }

    public static ChestShopPlayerDto toChestShopPlayerDto(Player player) {
        if (player == null) {
            return null;
        }

        ChestShopPlayerDto result = new ChestShopPlayerDto();
        result.setName(player.getName());
        return result;
    }

    public static ChestShopDto toChestShopDto(ChestShop chestShop) {
        if (chestShop == null) {
            return null;
        }

        ChestShopDto result = new ChestShopDto();
        result.setServer(chestShop.server);
        result.setLocation(chestShop.getLocation());
        result.setMaterial(chestShop.material);
        result.setOwner(toChestShopPlayerDto(chestShop.owner));
        result.setTown(toChestShopRegionDto(chestShop.town));
        result.setQuantity(chestShop.quantity);
        result.setQuantityAvailable(chestShop.quantityAvailable);
        result.setBuyPrice(chestShop.buyPrice);
        result.setSellPrice(chestShop.sellPrice);
        result.setBuyPriceEach(chestShop.buyPriceEach);
        result.setSellPriceEach(chestShop.sellPriceEach);
        result.setFull(chestShop.isFull);
        result.setBuySign(chestShop.isBuySign);
        result.setSellSign(chestShop.isSellSign);

        return result;
    }
}
