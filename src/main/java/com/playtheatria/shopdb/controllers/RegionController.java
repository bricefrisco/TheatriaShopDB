package com.playtheatria.shopdb.controllers;

import com.playtheatria.shopdb.database.ChestShop;
import com.playtheatria.shopdb.database.Player;
import com.playtheatria.shopdb.database.Region;
import com.playtheatria.shopdb.models.PaginatedResponse;
import com.playtheatria.shopdb.models.exceptions.ExceptionMessage;
import com.playtheatria.shopdb.models.exceptions.SDBIllegalArgumentException;
import com.playtheatria.shopdb.models.exceptions.SDBNotFoundException;
import com.playtheatria.shopdb.models.players.PlayerDto;
import com.playtheatria.shopdb.models.players.PlayerMapper;
import com.playtheatria.shopdb.models.regions.RegionDto;
import com.playtheatria.shopdb.models.regions.RegionMapper;
import com.playtheatria.shopdb.models.regions.RegionRequest;
import com.playtheatria.shopdb.services.APIKeyValidator;
import com.playtheatria.shopdb.services.ChestShopService;
import com.playtheatria.shopdb.services.Pagination;
import com.playtheatria.shopdb.services.RegionService;
import com.playtheatria.shopdb.models.chestshops.*;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@Path("/regions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RegionController {
    Logger LOGGER = LoggerFactory.getLogger(RegionController.class);

    @Inject
    RegionService regionService;

    @Inject
    APIKeyValidator apiKeyValidator;

    @Inject
    ChestShopService chestShopService;

    @GET
    @Transactional
    public PaginatedResponse<RegionDto> getRegions(
            @DefaultValue("1") @QueryParam("page") Integer page,
            @DefaultValue("6") @QueryParam("pageSize") Integer pageSize,
            @QueryParam("server") Server server,
            @DefaultValue("false") @QueryParam("active") Boolean active,
            @DefaultValue("") @QueryParam("name") String name,
            @DefaultValue("name") @QueryParam("sortBy") SortBy sortBy
    ) {
        LOGGER.info("GET /regions");
        if (page < 1) throw new SDBIllegalArgumentException(ExceptionMessage.INVALID_PAGE);
        if (pageSize < 1 || pageSize > 100) throw new SDBIllegalArgumentException(ExceptionMessage.INVALID_PAGE_SIZE);

        PanacheQuery<Region> regions = Region.findByServerAndName(server, active, name, sortBy);
        long totalResults = Region.findByServerAndName(server, active, name, SortBy.NAME).count();
        List<RegionDto> results = regions.page(page - 1, pageSize).stream().map(RegionMapper::toRegionDto).collect(Collectors.toList());

        return new PaginatedResponse<>(page, Pagination.getNumPages(pageSize, totalResults), totalResults, results);
    }

    @GET
    @Path("region-names")
    public List<PanacheEntityBase> getRegionNames(
            @QueryParam("server") Server server,
            @DefaultValue("false") @QueryParam("active") Boolean active) {
        LOGGER.info("GET /region-names");
        return Region.findRegionNames(server, active);
    }

    @GET
    @Path("{server}/{name}")
    public RegionDto getRegion(
            @PathParam("server") Server server,
            @PathParam("name") String name
    ) {
        LOGGER.info("GET /regions/" + server + "/" + name);

        if (name == null) throw new SDBIllegalArgumentException(ExceptionMessage.EMPTY_REGION_NAME);
        if (server == null) throw new SDBIllegalArgumentException(ExceptionMessage.EMPTY_SERVER_NAME);

        Region region = Region.findByServerAndName(server, name);
        if (region == null) throw new SDBNotFoundException(String.format(ExceptionMessage.REGION_NOT_FOUND, name, server));

        return RegionMapper.toRegionDto(Region.findByServerAndName(server, name));
    }

    @GET
    @Path("{server}/{name}/players")
    public PaginatedResponse<PlayerDto> getRegionOwners(
            @PathParam("server") Server server,
            @PathParam("name") String name,
            @DefaultValue("1") @QueryParam("page") Integer page,
            @DefaultValue("6") @QueryParam("pageSize") Integer pageSize) {
        LOGGER.info("GET /regions/" + server + "/" + name + "/players");

        if (page < 1) throw new SDBIllegalArgumentException(ExceptionMessage.INVALID_PAGE);
        if (pageSize < 1 || pageSize > 100) throw new SDBIllegalArgumentException(ExceptionMessage.INVALID_PAGE_SIZE);
        if (name == null) throw new SDBIllegalArgumentException(ExceptionMessage.EMPTY_REGION_NAME);
        if (server == null) throw new SDBIllegalArgumentException(ExceptionMessage.EMPTY_SERVER_NAME);

        Region region = Region.findByServerAndName(server, name);
        if (region == null) throw new SDBNotFoundException(String.format(ExceptionMessage.REGION_NOT_FOUND, name, server));

        List<Player> players = Pagination.getPage(region.mayors, page, pageSize);
        int totalResults = players.size();
        List<PlayerDto> results = players.stream().map(PlayerMapper::toPlayerDto).collect(Collectors.toList());

        return new PaginatedResponse<>(page, Pagination.getNumPages(pageSize, totalResults), totalResults, results);
    }

    @GET
    @Path("{server}/{name}/chest-shops")
    public PaginatedResponse<ChestShopDto> getRegionChestShops(
            @PathParam("server") Server server,
            @PathParam("name") String name,
            @DefaultValue("1") @QueryParam("page") Integer page,
            @DefaultValue("6") @QueryParam("pageSize") Integer pageSize,
            @DefaultValue("buy") @QueryParam("tradeType") TradeType tradeType) {
        LOGGER.info("GET /regions/" + server + "/" + name + "/chest-shops");

        if (page < 1) throw new SDBIllegalArgumentException(ExceptionMessage.INVALID_PAGE);
        if (pageSize < 1 || pageSize > 100) throw new SDBIllegalArgumentException(ExceptionMessage.INVALID_PAGE_SIZE);

        Region region = Region.findByServerAndName(server, name);
        if (region == null) throw new SDBNotFoundException(String.format(ExceptionMessage.REGION_NOT_FOUND, name, server));

        PanacheQuery<ChestShop> chestShops = ChestShop.findInRegion(region, tradeType);
        long totalResults = chestShops.count();
        List<ChestShopDto> results = chestShops.page(page - 1, pageSize).stream().map(ChestShopMapper::toChestShopDto).collect(Collectors.toList());

        return new PaginatedResponse<>(page, Pagination.getNumPages(pageSize, totalResults), totalResults, results);
    }

    @PUT
    @Transactional
    @Consumes("application/json")
    public String addRegion(RegionRequest request, @HeaderParam("Authorization") String authHeader) throws Exception {
        apiKeyValidator.validateAPIKey(authHeader);
        Region r = regionService.listRegion(request);
        chestShopService.linkAndShowChestShops(r);
        return "Successfully listed region " + request.getName();
    }

    @DELETE
    @Transactional
    @Consumes("application/json")
    public String removeRegion(RegionRequest request, @HeaderParam("Authorization") String authHeader) throws Exception {
        apiKeyValidator.validateAPIKey(authHeader);
        Region r = regionService.unlistRegion(request);
        chestShopService.linkAndHideChestShops(r);
        return "Successfully unlisted region " + request.getName();
    }

}
