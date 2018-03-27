package edu.odu.cs.gold.controller;

import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import edu.odu.cs.gold.model.*;
import edu.odu.cs.gold.repository.*;
import edu.odu.cs.gold.service.GoogleMapService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.google.maps.model.*;

import java.util.*;

@Controller
@RequestMapping("/analytics")
public class AnalyticsController {

    private GarageRepository garageRepository;
    private BuildingRepository buildingRepository;
    private ParkingSpaceRepository parkingSpaceRepository;
    private TravelDistanceDurationRepository travelDistanceDurationRepository;
    private GoogleMapService googleMapService;
    private PermitTypeRepository permitTypeRepository;
    private SpaceTypeRepository spaceTypeRepository;
    private RecommendationRepository recommendationRepository;

    public AnalyticsController(GarageRepository garageRepository,
                               BuildingRepository buildingRepository,
                               ParkingSpaceRepository parkingSpaceRepository,
                               TravelDistanceDurationRepository travelDistanceDurationRepository,
                               GoogleMapService googleMapService,
                               PermitTypeRepository permitTypeRepository,
                               SpaceTypeRepository spaceTypeRepository,
                               RecommendationRepository recommendationRepository) {
        this.garageRepository = garageRepository;
        this.buildingRepository = buildingRepository;
        this.parkingSpaceRepository = parkingSpaceRepository;
        this.travelDistanceDurationRepository = travelDistanceDurationRepository;
        this.googleMapService = googleMapService;
        this.permitTypeRepository = permitTypeRepository;
        this.spaceTypeRepository = spaceTypeRepository;
        this.recommendationRepository = recommendationRepository;
    }

    @GetMapping({"","/","/index"})
    public String index(Model model) {
        List<Building> buildings = new ArrayList<>(buildingRepository.findAll());
        buildings.sort(Comparator.comparing(Building::getName));
        model.addAttribute("buildings", buildings);
        return "analytics/index";
    }

    @GetMapping("/locate")
    public String locate(Model model) {
        List<Building> buildings = new ArrayList<>(buildingRepository.findAll());
        buildings.sort(Comparator.comparing(Building::getName));

        List<PermitType> permitTypes = new ArrayList<>(permitTypeRepository.findAll());
        List<SpaceType> spaceTypes = new ArrayList<>(spaceTypeRepository.findAll());

        model.addAttribute("buildings", buildings);
        model.addAttribute("spaceTypes", spaceTypes);
        model.addAttribute("permitTypes", permitTypes);
        return "analytics/locate";
    }

    @PostMapping("/locate")
    public String locate(@RequestParam(name = "startingAddress", required = false) String startingAddress,
                         @RequestParam(name = "latitude", required = false) Double startingLatitude,
                         @RequestParam(name = "longitude", required = false) Double startingLongitude,
                         @RequestParam(name = "permitTypeKeys", required = false) List<String> permitTypeKeys,
                         @RequestParam(name = "spaceTypeKeys", required = false) List<String> spaceTypeKeys,
                         @RequestParam(name = "destinationBuildingId", required = false) String destinationBuildingId,
                         @RequestParam(name = "minSpaces", required = false) Integer minSpaces,
                         Model model) {

        Location startingLocation = new Location(startingLatitude, startingLongitude);
        Building destinationBuilding = buildingRepository.findByKey(destinationBuildingId);

        Predicate permitPredicate = null;
        if (permitTypeKeys != null) {
            List<Predicate> predicates = new ArrayList<>();
            for (String permitTypeKey : permitTypeKeys) {
                predicates.add(Predicates.equal("permitTypeKey", permitTypeKey));
            }
            permitPredicate = Predicates.or(predicates.toArray(new Predicate[0]));
        }

        List<Recommendation> recommendations = new ArrayList<>();

        List<Garage> garages = new ArrayList<>(garageRepository.findAll());

        for (Garage garage : garages) {
            int availabilityCount = 0;
            int totalCount = 0;
            if (permitPredicate != null) {

                // Availability Count
                Predicate availabilityCountPredicate = Predicates.and(
                        Predicates.equal("garageKey", garage.getGarageKey()),
                        Predicates.equal("available", true),
                        permitPredicate
                );
                availabilityCount = parkingSpaceRepository.countByPredicate(availabilityCountPredicate);

                // Total Count
                Predicate totalCountPredicate = Predicates.and(
                    Predicates.equal("garageKey", garage.getGarageKey()),
                    permitPredicate
                );
                totalCount = parkingSpaceRepository.countByPredicate(totalCountPredicate);
            }
            else {
                availabilityCount = garage.getAvailableSpaces();
            }
            if (minSpaces <= availabilityCount) {
                Recommendation recommendation = new Recommendation();
                recommendation.generateRecommendationKey();
                recommendation.setStartingAddress(startingAddress);
                recommendation.setGarage(garage);
                recommendation.setDestinationBuilding(destinationBuilding);

                recommendation.setAvailabilityCount(availabilityCount);
                recommendation.setTotalCount(totalCount);

                DistanceDuration startingAddressToGarage = googleMapService.getDistanceDuration(startingLocation, garage.getLocation(), TravelMode.DRIVING);
                recommendation.setStartingAddressToGarage(startingAddressToGarage);

                DistanceDuration garageToDestinationBuilding = googleMapService.getDistanceDuration(garage.getLocation(), destinationBuilding.getLocation(), TravelMode.WALKING);
                recommendation.setGarageToDestinationBuilding(garageToDestinationBuilding);

                // Set Total Distance
                recommendation.setTotalDistanceValue(recommendation.getStartingAddressToGarageDistanceValue() + recommendation.getGarageToDestinationBuildingDistanceValue());

                // Set Total Duration
                recommendation.setTotalDurationValue(recommendation.getStartingAddressToGarageDurationValue() + recommendation.getGarageToDestinationBuildingDurationValue());

                recommendations.add(recommendation);
                //recommendationRepository.save(recommendation);
            }
        }

        // Default sort by closest Garage to Destination Building
        recommendations.sort(Comparator.comparing(Recommendation::getGarageToDestinationBuildingDistanceValue));

        List<PermitType> permitTypes = new ArrayList<> ();

        // Get the Permit Type objects from the keys
        if (permitTypeKeys != null) {
            Set<String> permitTypeKeySet = new HashSet<String> (permitTypeKeys);
            permitTypes = new ArrayList<> (permitTypeRepository.findByKeys(permitTypeKeySet));
        }

        model.addAttribute("startingAddress", startingAddress);
        model.addAttribute("startingLatitude", startingLatitude);
        model.addAttribute("startingLongitude", startingLongitude);
        model.addAttribute("permitTypes", permitTypes);
        model.addAttribute("destinationBuilding", destinationBuilding);
        model.addAttribute("recommendations", recommendations);

        return "analytics/recommendation";
    }

    @GetMapping({"/building/{buildingKey}"})
    public String building(@PathVariable String buildingKey, Model model) {
        Building building = buildingRepository.findByKey(buildingKey);
        Map<String, Garage> garageMap = garageRepository.findAllMap();

        Predicate predicate = Predicates.equal("buildingKey", buildingKey);
        List<TravelDistanceDuration> travelDistanceDurations = travelDistanceDurationRepository.findByPredicate(predicate);
        travelDistanceDurations.sort(Comparator.comparing(TravelDistanceDuration::getDurationValue));
        for (TravelDistanceDuration travelDistanceDuration : travelDistanceDurations) {
            travelDistanceDuration.setGarageName(garageMap.get(travelDistanceDuration.getGarageKey()).getName());
            travelDistanceDuration.setAvailableSpaces(garageMap.get(travelDistanceDuration.getGarageKey()).getAvailableSpaces());
            travelDistanceDuration.setTotalSpaces(garageMap.get(travelDistanceDuration.getGarageKey()).getTotalSpaces());
            travelDistanceDuration.setCapacity(garageMap.get(travelDistanceDuration.getGarageKey()).getCapacity());
        }

        model.addAttribute("building", building);
        model.addAttribute("travelDistanceDurations", travelDistanceDurations);
        return "analytics/building";
    }
}
