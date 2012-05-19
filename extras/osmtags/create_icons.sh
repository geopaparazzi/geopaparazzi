#!/bin/sh


SIZE=128
PREFIX=/home/luca/github/Open-SVG-Map-Icons

cd accommodation
  generateonepng.sh -f $PREFIX/svg/accommodation/alpinehut.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/accommodation/bed_and_breakfast.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/accommodation/camping.svg -c '#0000ff' -s $SIZE
  mv camping.n.${SIZE}.png camp_site.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/accommodation/caravan_park.svg -c '#0000ff' -s $SIZE
  mv caravan_park.n.${SIZE}.png caravan_site.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/accommodation/chalet.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/accommodation/youth_hostel.svg -c '#0000ff' -s $SIZE
  mv youth_hostel.n.${SIZE}.png hostel.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/accommodation/hotel.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/accommodation/motel.svg -c '#0000ff' -s $SIZE
  rm -f *.p.* *.glow.* 
cd ..

cd barrier
  generateonepng.sh -f $PREFIX/svg/barrier/cycle_barrier.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/barrier/blocks.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/barrier/bollard.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/barrier/entrance.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/barrier/gate.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/barrier/kissing_gate.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/barrier/lift_gate.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/barrier/toll_booth.svg -c '#0000ff' -s $SIZE
  rm -f *.p.* *.glow.* 
cd ..

cd facilities
  generateonepng.sh -f $PREFIX/svg/money/atm2.svg -c '#0000ff' -s $SIZE
  mv atm2.n.${SIZE}.png atm.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/money/bank2.svg -c '#0000ff' -s $SIZE
  mv bank2.n.${SIZE}.png bank.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/money/currency_exchange.svg -c '#0000ff' -s $SIZE
  mv currency_exchang.n.${SIZE}.png bureau_de_change.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/amenity/library.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/amenity/post_box.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/amenity/recycling.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/amenity/waste_bin.svg -c '#0000ff' -s $SIZE
  mv waste_bin.n.${SIZE}.png waste_basket.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/amenity/toilets.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/amenity/telephone.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/amenity/bench.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/tourist/clock.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/amenity/firestation.svg -c '#0000ff' -s $SIZE
  mv firestation.n.${SIZE}.png fire_hydrant.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/food/drinkingtap.svg -c '#0000ff' -s $SIZE
  mv drinkingtap.n.${SIZE}.png drinking_water.n.${SIZE}.png
  rm -f *.p.* *.glow.* 
cd ..

cd food
  generateonepng.sh -f $PREFIX/svg/food/bar.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/food/cafe.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/food/fastfood.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/food/pub.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/food/restaurant.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/food/ice_cream.svg -c '#0000ff' -s $SIZE
  rm -f *.p.* *.glow.* 
cd ..

cd geo
  generateonepng.sh -f $PREFIX/svg/poi/mountain_pass.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/poi/peak2.svg -c '#0000ff' -s $SIZE
  mv peak2.n.${SIZE}.png peak.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/landuse/deciduous.svg -c '#0000ff' -s $SIZE
  mv deciduous.n.${SIZE}.png tree.n.${SIZE}.png
  rm -f *.p.* *.glow.* 
cd ..

cd health
  generateonepng.sh -f $PREFIX/svg/health/hospital.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/health/dentist.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/health/doctors.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/health/veterinary.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/health/pharmacy_dispencing.svg -c '#0000ff' -s $SIZE
  mv pharmacy_dispencing.n.${SIZE}.png pharmacy.n.${SIZE}.png
  rm -f *.p.* *.glow.* 
cd ..

cd history
  generateonepng.sh -f $PREFIX/svg/tourist/archaeological.svg -c '#0000ff' -s $SIZE
  mv archaeological.n.${SIZE}.png archaeological_site.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/tourist/battlefield.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/tourist/castle2.svg -c '#0000ff' -s $SIZE
  mv castle2.n.${SIZE}.png castle.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/tourist/memorial.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/tourist/monument.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/tourist/ruin.svg -c '#0000ff' -s $SIZE
  mv ruin.n.${SIZE}.png ruins.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/tourist/wayside_cross.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/tourist/wayside_shrine.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/barrier/bollard.svg -c '#0000ff' -s $SIZE
  mv bollard.n.${SIZE}.png boundary_stone.n.${SIZE}.png
  rm -f *.p.* *.glow.* 
cd ..

cd place
  generateonepng.sh -f $PREFIX/svg/poi/place_city.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/poi/place_town.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/poi/place_village.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/poi/place_hamlet.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/poi/place_suburb.svg -c '#0000ff' -s $SIZE
  rm -f *.p.* *.glow.* 
  for i in `ls *.n.${SIZE}.png`; do
    place=`echo $i | cut -d'.' -f1 | cut -d"_" -f2`;
    mv $i ${place}.n.${SIZE}.png
  done
cd ..

cd public_building
  generateonepng.sh -f $PREFIX/svg/poi/embassy2.svg -c '#0000ff' -s $SIZE
  mv embassy2.n.${SIZE}.png embassy.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/amenity/public_building.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/amenity/prison.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/amenity/town_hall2.svg -c '#0000ff' -s $SIZE
  mv town_hall2.n.${SIZE}.png town_hall.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/amenity/police2.svg -c '#0000ff' -s $SIZE
  mv police2.n.${SIZE}.png police.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/amenity/firestation3.svg -c '#0000ff' -s $SIZE
  mv firestation3.n.${SIZE}.png firestation.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/amenity/post_office.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/amenity/court.svg -c '#0000ff' -s $SIZE
  mv court.n.${SIZE}.png courthouse.n.${SIZE}.png
  rm -f *.p.* *.glow.* 
cd ..

cd tourism
  generateonepng.sh -f $PREFIX/svg/tourist/art_gallery.svg -c '#0000ff' -s $SIZE
  mv art_gallery.n.${SIZE}.png arts_centre.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/tourist/art_gallery2.svg -c '#0000ff' -s $SIZE
  mv art_gallery2.n.${SIZE}.png artwork.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/tourist/board.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/tourist/guidepost.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/tourist/map2.svg -c '#0000ff' -s $SIZE
  mv map2.n.${SIZE}.png map.n.${SIZE}.png 
  generateonepng.sh -f $PREFIX/svg/tourist/museum.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/tourist/information.svg -c '#0000ff' -s $SIZE
  mv information.n.${SIZE}.png office.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/tourist/picnic.svg -c '#0000ff' -s $SIZE
  mv picnic.n.${SIZE}.png picnic_site.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/tourist/theatre.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/tourist/view_point.svg -c '#0000ff' -s $SIZE
  mv view_point.n.${SIZE}.png viewpoint.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/place_of_worship/unknown.svg -c '#0000ff' -s $SIZE
  mv unknown.n.${SIZE}.png place_of_worship.n.${SIZE}.png
  rm -f *.p.* *.glow.* 
cd ..

cd shop
  generateonepng.sh -f $PREFIX/svg/shopping/bakery.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/shopping/book.svg -c '#0000ff' -s $SIZE
  mv book.n.${SIZE}.png books.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/shopping/butcher.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/shopping/clothes.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/shopping/computer.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/shopping/fish.svg -c '#0000ff' -s $SIZE
  mv fish.n.${SIZE}.png seefood.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/shopping/florist.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/shopping/deli.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/shopping/diy.svg -c '#0000ff' -s $SIZE
  mv diy.n.${SIZE}.png doityourself.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/shopping/greengrocer.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/shopping/hairdresser.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/shopping/hifi.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/shopping/jewelry.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/shopping/kiosk.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/shopping/laundrette.svg -c '#0000ff' -s $SIZE
  mv laundrette.n.${SIZE}.png laundry.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/shopping/mobile_phone.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/shopping/music.svg -c '#0000ff' -s $SIZE
  mv music.n.${SIZE}.png musical_instrument.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/shopping/newspaper.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/shopping/pet2.svg -c '#0000ff' -s $SIZE
  mv pet2.n.${SIZE}.png pet.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/shopping/photo.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/shopping/shoes.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/shopping/sport.svg -c '#0000ff' -s $SIZE

  generateonepng.sh -f $PREFIX/svg/shopping/supermarket.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/shopping/tobacco.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/shopping/video_rental.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/shopping/vending_machine.svg -c '#0000ff' -s $SIZE
  rm -f *.p.* *.glow.* 
cd ..

cd transport_bicycle
  generateonepng.sh -f $PREFIX/svg/transport/parking_bicycle.svg -c '#0000ff' -s $SIZE
  mv parking_bicycle.n.${SIZE}.png bicycle_parking.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/transport/rental_bicycle.svg -c '#0000ff' -s $SIZE
  mv rental_bicycle.n.${SIZE}.png bicycle_rental.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/shopping/bicycle2.svg -c '#0000ff' -s $SIZE
  mv bicycle2.n.${SIZE}.png bicycle_shop.n.${SIZE}.png
  rm -f *.p.* *.glow.*
cd ..


cd transport_motor
  CAR_DEALER
  generateonepng.sh -f $PREFIX/svg/transport/parking_car.svg -c '#0000ff' -s $SIZE
  mv parking_car.n.${SIZE}.png car_parking.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/transport/rental_car.svg -c '#0000ff' -s $SIZE
  mv rental_car.n.${SIZE}.png car_rental.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/shopping/car_repair2.svg -c '#0000ff' -s $SIZE
  mv car_repair2.n.${SIZE}.png car_repair.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/transport/car_share.svg -c '#0000ff' -s $SIZE
  mv car_share.n.${SIZE}.png car_sharing.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/transport/car_wash.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/transport/charging_station.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/transport/zebra_crossing.svg -c '#0000ff' -s $SIZE
  mv zebra_crossing.n.${SIZE}.png crossing.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/transport/emergency_phone.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/transport/fuel.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/transport/parking_motorcycle.svg -c '#0000ff' -s $SIZE
  mv parking_motorcycle.n.${SIZE}.png motorcycle_parking.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/transport/stop.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/transport/traffic_lights.svg -c '#0000ff' -s $SIZE
  mv traffic_lights.n.${SIZE}.png traffic_signals.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/shopping/tyres.svg -c '#0000ff' -s $SIZE
  rm -f *.p.* *.glow.*
cd ..

cd transport_public
  generateonepng.sh -f $PREFIX/svg/transport/bus_station.svg -c '#0000ff' -s $SIZE
  generateonepng.sh -f $PREFIX/svg/transport/bus_stop2.svg -c '#0000ff' -s $SIZE
  mv bus_stop2.n.${SIZE}.png bus_stop.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/transport/train_station2.svg -c '#0000ff' -s $SIZE
  mv train_station2.n.${SIZE}.png halt.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/transport/level_crossing.svg -c '#0000ff' -s $SIZE  
  generateonepng.sh -f $PREFIX/svg/transport/train_station.svg -c '#0000ff' -s $SIZE
  mv train_station.n.${SIZE}.png station.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/transport/subway2.svg -c '#0000ff' -s $SIZE
  mv subway2.n.${SIZE}.png subway_entrance.n.${SIZE}.png  
  generateonepng.sh -f $PREFIX/svg/transport/taxi_rank.svg -c '#0000ff' -s $SIZE
  mv taxi_rank.n.${SIZE}.png taxi.n.${SIZE}.png
  generateonepng.sh -f $PREFIX/svg/transport/tram_stop.svg -c '#0000ff' -s $SIZE
  rm -f *.p.* *.glow.*
cd ..