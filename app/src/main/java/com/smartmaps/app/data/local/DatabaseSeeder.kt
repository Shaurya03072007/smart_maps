package com.smartmaps.app.data.local

import com.smartmaps.app.data.local.dao.ServiceCategoryDao
import com.smartmaps.app.data.local.dao.ServiceDao
import com.smartmaps.app.data.local.entity.ServiceCategoryEntity
import com.smartmaps.app.data.local.entity.ServiceEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds the Room database with 100+ services taxonomy on first launch.
 * This runs once when the service count is 0.
 */
@Singleton
class DatabaseSeeder @Inject constructor(
    private val categoryDao: ServiceCategoryDao,
    private val serviceDao: ServiceDao,
    private val providerDao: com.smartmaps.app.data.local.dao.ProviderDao,
    private val providerServiceDao: com.smartmaps.app.data.local.dao.ProviderServiceDao
) {

    suspend fun seedIfEmpty() {
        if (serviceDao.count() == 0) {
            seedCategories()
            seedServices()
        }
        seedProviders()
    }

    private suspend fun seedCategories() {
        val categories = listOf(
            ServiceCategoryEntity("home_services", "Home Services", "🏠", 0xFF1976D2, 0),
            ServiceCategoryEntity("appliance_repair", "Appliance Repair", "🔧", 0xFF7B1FA2, 1),
            ServiceCategoryEntity("vehicle", "Vehicle Services", "🚗", 0xFFD32F2F, 2),
            ServiceCategoryEntity("technology", "Technology", "💻", 0xFF00796B, 3),
            ServiceCategoryEntity("beauty_wellness", "Beauty & Wellness", "💆", 0xFFE91E63, 4),
            ServiceCategoryEntity("healthcare", "Healthcare", "🏥", 0xFF388E3C, 5),
            ServiceCategoryEntity("education", "Education", "📚", 0xFFF57C00, 6),
            ServiceCategoryEntity("events", "Events & Entertainment", "🎉", 0xFF5C6BC0, 7),
            ServiceCategoryEntity("professional", "Professional Services", "💼", 0xFF455A64, 8),
            ServiceCategoryEntity("logistics", "Logistics & Delivery", "📦", 0xFF795548, 9),
            ServiceCategoryEntity("construction", "Construction", "🏗️", 0xFF616161, 10),
            ServiceCategoryEntity("pet", "Pet Services", "🐾", 0xFF8D6E63, 11),
            ServiceCategoryEntity("agriculture", "Agriculture", "🌿", 0xFF558B2F, 12),
            ServiceCategoryEntity("emergency", "Emergency Services", "🚨", 0xFFB71C1C, 13),
        )
        categoryDao.insertAll(categories)
    }

    private suspend fun seedServices() {
        val services = mutableListOf<ServiceEntity>()

        // ═══════════════════════════════
        // HOME SERVICES
        // ═══════════════════════════════
        services.addAll(listOf(
            ServiceEntity("plumbing_tap_repair", "home_services", "Tap Repair", "Repair leaking or broken taps", "🚿",
                "tap,faucet,leaking,dripping,water,repair", "tap repair,faucet fix,leaking tap,dripping tap", 1, 30, 90, 150, 400, "plumbing"),
            ServiceEntity("plumbing_pipe_repair", "home_services", "Pipe Repair", "Fix burst or leaking pipes", "🔧",
                "pipe,burst,leak,water,broken pipe,plumber", "pipe leak,burst pipe,water pipe,pipe fix", 1, 60, 180, 300, 800, "plumbing"),
            ServiceEntity("plumbing_drain_cleaning", "home_services", "Drain Cleaning", "Clear blocked drains and sinks", "🪣",
                "drain,clog,blocked,sink,bathroom,kitchen,pipe", "drain block,clogged drain,sink blocked,drain cleaning", 1, 30, 120, 200, 600, "plumbing,drain_cleaning"),
            ServiceEntity("plumbing_toilet_repair", "home_services", "Toilet Repair", "Fix toilet flush, seat or tank issues", "🚽",
                "toilet,flush,commode,seat,tank,broken,leaking", "toilet fix,flush repair,commode repair,toilet not flushing", 1, 30, 90, 150, 500, "plumbing"),
            ServiceEntity("plumbing_water_heater", "home_services", "Water Heater Repair", "Repair or install geysers and water heaters", "♨️",
                "geyser,water heater,hot water,heater repair,install", "geyser repair,water heater fix,hot water not coming", 1, 60, 150, 300, 700, "plumbing,electrical"),
            ServiceEntity("electrical_wiring", "home_services", "Electrical Wiring", "New wiring or rewiring of house/office", "⚡",
                "wiring,electrical,rewire,new connection,house wiring", "wiring work,electrical wiring,house wiring", 2, 120, 480, 800, 3000, "electrical,wiring"),
            ServiceEntity("electrical_fan_repair", "home_services", "Fan Repair", "Repair or replace ceiling and wall fans", "🌀",
                "fan,ceiling fan,wall fan,fan not working,fan repair", "fan fix,ceiling fan repair,fan installation", 1, 30, 90, 150, 400, "electrical,fan_repair"),
            ServiceEntity("electrical_switch_repair", "home_services", "Switch & Socket Repair", "Fix broken switches, sockets and MCBs", "🔌",
                "switch,socket,plug,mcb,switchboard,electrical,broken", "switch repair,socket fix,plug point,switchboard repair", 1, 20, 60, 100, 300, "electrical"),
            ServiceEntity("electrical_fault_detection", "home_services", "Electrical Fault Detection", "Diagnose and fix electrical faults, short circuits", "🔍",
                "fault,short circuit,trip,breaker,electrical problem,no power", "electrical fault,short circuit,power problem,trip breaker", 1, 60, 180, 400, 1000, "electrical"),
            ServiceEntity("ac_repair", "home_services", "AC Repair", "Fix air conditioner issues and faults", "❄️",
                "ac,air conditioner,cooling,not cooling,ac repair,split ac,window ac", "ac fix,ac not cooling,air conditioner repair,ac problem", 1, 60, 180, 400, 1200, "ac_technician"),
            ServiceEntity("ac_installation", "home_services", "AC Installation", "Install split or window air conditioners", "🔩",
                "ac install,air conditioner installation,new ac,split ac install", "ac installation,new ac setup,air conditioner install", 2, 120, 240, 800, 2000, "ac_technician"),
            ServiceEntity("ac_gas_check", "home_services", "AC Gas Refill", "Check and refill AC refrigerant gas", "💨",
                "ac gas,refrigerant,gas refill,ac not cold,gas charge", "ac gas refill,refrigerant fill,ac gas check", 1, 60, 120, 500, 1500, "ac_technician"),
            ServiceEntity("ac_cleaning", "home_services", "AC Deep Cleaning", "Service and clean split/window AC units", "🧹",
                "ac clean,ac service,ac maintenance,deep clean ac", "ac cleaning,ac service,ac wash", 1, 60, 120, 400, 800, "ac_technician"),
            ServiceEntity("carpentry_furniture", "home_services", "Furniture Repair", "Repair or assemble wooden furniture", "🪑",
                "furniture,carpenter,wood,chair,table,almirah,repair", "furniture repair,carpenter,wood work,furniture assembly", 1, 60, 240, 300, 1500, "carpentry"),
            ServiceEntity("carpentry_door_window", "home_services", "Door & Window Repair", "Fix doors, windows, hinges and locks", "🚪",
                "door,window,hinge,lock,carpenter,stuck,repair", "door repair,window fix,hinge repair,door not closing", 1, 60, 180, 300, 1000, "carpentry"),
            ServiceEntity("painting_interior", "home_services", "Interior Painting", "Paint walls, ceilings and interior rooms", "🎨",
                "painting,paint,interior,wall,ceiling,room,house painting", "interior painting,wall painting,house painting,room paint", 3, 480, 1920, 5000, 25000, "painting"),
            ServiceEntity("painting_exterior", "home_services", "Exterior Painting", "Paint building exterior and facade", "🏚️",
                "exterior painting,outside,wall,building,weather coat,paint", "exterior painting,outside painting,building paint", 4, 960, 2880, 15000, 60000, "painting"),
            ServiceEntity("cleaning_home", "home_services", "Home Deep Cleaning", "Professional deep cleaning of entire home", "🧽",
                "cleaning,deep clean,home,house,bathroom,kitchen,mop,vacuum", "home cleaning,deep cleaning,house cleaning,full cleaning", 2, 180, 480, 1000, 5000, "cleaning"),
            ServiceEntity("cleaning_bathroom", "home_services", "Bathroom Cleaning", "Deep clean bathrooms and toilets", "🚿",
                "bathroom clean,toilet clean,sanitation,tiles,bathroom", "bathroom cleaning,toilet cleaning,washroom clean", 1, 60, 120, 400, 1200, "cleaning"),
            ServiceEntity("cleaning_sofa", "home_services", "Sofa & Carpet Cleaning", "Dry or steam clean sofas, carpets and upholstery", "🛋️",
                "sofa clean,carpet,upholstery,steam clean,sofa wash", "sofa cleaning,carpet cleaning,upholstery cleaning", 2, 120, 240, 800, 3000, "cleaning"),
            ServiceEntity("pest_control_cockroach", "home_services", "Cockroach Treatment", "Chemical treatment to eliminate cockroaches", "🪳",
                "cockroach,pest,insect,treatment,spray,exterminator", "cockroach control,pest control cockroach,anti cockroach", 1, 60, 120, 500, 1500, "pest_control"),
            ServiceEntity("pest_control_termite", "home_services", "Termite Control", "Termite proofing treatment for home", "🐜",
                "termite,white ant,wood borer,anti termite,pest control", "termite control,termite treatment,anti termite spray", 2, 120, 480, 2000, 8000, "pest_control"),
            ServiceEntity("pest_control_mosquito", "home_services", "Mosquito & Insect Control", "Spray treatment for mosquitoes and general insects", "🦟",
                "mosquito,insect,spray,fogging,pest control,dengue", "mosquito control,insect control,pest spray,fogging", 1, 60, 120, 600, 2000, "pest_control"),
            ServiceEntity("ro_water_purifier", "home_services", "RO Water Purifier Service", "Install, repair and service RO water purifiers", "💧",
                "ro,water purifier,reverse osmosis,kent,aquaguard,repair,install,service", "ro repair,water purifier service,ro installation,aquaguard repair", 1, 60, 150, 300, 1000, "ro_technician"),
            ServiceEntity("cctv_installation", "home_services", "CCTV Installation", "Install security cameras and surveillance systems", "📷",
                "cctv,camera,security,surveillance,installation,cctv repair", "cctv install,security camera,cctv setup,surveillance camera", 2, 120, 360, 1000, 5000, "cctv_technician"),
            ServiceEntity("gardening_maintenance", "home_services", "Garden Maintenance", "Lawn mowing, pruning and garden care", "🌿",
                "garden,plants,lawn,mowing,pruning,trimming,gardener", "garden maintenance,lawn mowing,plant care,gardening", 1, 60, 240, 300, 1500, "gardening"),
            ServiceEntity("interior_design", "home_services", "Interior Design Consultation", "Professional interior design and space planning", "🏡",
                "interior design,decor,space planning,home design,interior decorator", "interior design,home decor,space planning", 1, 120, 240, 2000, 10000, "interior_designer"),
            ServiceEntity("laundry_pickup", "home_services", "Laundry & Dry Cleaning", "Pickup, wash and delivery of clothes", "👕",
                "laundry,dry clean,clothes wash,ironing,pickup,delivery,wash", "laundry pickup,dry cleaning,clothes washing,ironing", 1, 60, 120, 200, 800, "laundry"),
            ServiceEntity("tailoring", "home_services", "Tailoring & Alterations", "Custom stitching and garment alterations", "✂️",
                "tailoring,stitch,alteration,clothes,garment,tailor,dress", "tailoring,clothes alteration,stitching,dress making", 1, 60, 480, 200, 2000, "tailoring"),
            ServiceEntity("solar_installation", "home_services", "Solar Panel Installation", "Install and commission rooftop solar systems", "☀️",
                "solar,panel,installation,rooftop,solar energy,inverter", "solar installation,solar panel setup,rooftop solar", 3, 480, 1440, 5000, 30000, "solar_technician"),
        ))

        // ═══════════════════════════════
        // APPLIANCE REPAIR
        // ═══════════════════════════════
        services.addAll(listOf(
            ServiceEntity("washing_machine_repair", "appliance_repair", "Washing Machine Repair", "Diagnose and fix all washing machine issues", "🫧",
                "washing machine,washer,laundry,machine not working,spin,drain", "washing machine repair,washer fix,washing machine service", 1, 60, 180, 300, 1000, "appliance_technician"),
            ServiceEntity("refrigerator_repair", "appliance_repair", "Refrigerator Repair", "Fix fridge cooling, motor and electrical issues", "🧊",
                "refrigerator,fridge,freeze,cooling,not cooling,compressor,repair", "fridge repair,refrigerator fix,fridge not cooling", 1, 60, 180, 400, 1500, "appliance_technician"),
            ServiceEntity("microwave_repair", "appliance_repair", "Microwave Repair", "Repair microwave oven faults and issues", "📡",
                "microwave,oven,microwave not working,heating,repair", "microwave repair,oven fix,microwave service", 1, 30, 90, 200, 800, "appliance_technician"),
            ServiceEntity("dishwasher_repair", "appliance_repair", "Dishwasher Repair", "Fix dishwasher drainage, motor and cleaning issues", "🍽️",
                "dishwasher,dishes,washing,repair,not draining,not cleaning", "dishwasher repair,dishwasher fix,dishwasher service", 1, 60, 150, 400, 1200, "appliance_technician"),
            ServiceEntity("tv_repair", "appliance_repair", "TV Repair", "Repair LED, LCD and Smart TV screens and boards", "📺",
                "tv,television,led,lcd,smart tv,screen,repair,no display", "tv repair,television fix,led tv repair,smart tv service", 1, 60, 180, 400, 2000, "electronics_technician"),
            ServiceEntity("chimney_repair", "appliance_repair", "Chimney Repair & Cleaning", "Service kitchen exhaust chimney and hoods", "🔥",
                "chimney,exhaust,kitchen hood,cleaning,repair,chimney service", "chimney repair,chimney cleaning,kitchen chimney service", 1, 60, 120, 400, 1000, "appliance_technician"),
            ServiceEntity("inverter_battery", "appliance_repair", "Inverter & Battery Service", "Service, repair or replace inverters and batteries", "🔋",
                "inverter,battery,ups,power backup,charging,not working", "inverter repair,battery service,ups repair", 1, 60, 180, 500, 2000, "electrical"),
        ))

        // ═══════════════════════════════
        // VEHICLE SERVICES
        // ═══════════════════════════════
        services.addAll(listOf(
            ServiceEntity("car_repair_general", "vehicle", "Car General Repair", "General car mechanical repair and servicing", "🚗",
                "car repair,automobile,mechanic,car service,engine,general repair", "car repair,car mechanic,auto repair,car service", 1, 60, 480, 500, 5000, "car_mechanic"),
            ServiceEntity("car_ac_repair", "vehicle", "Car AC Repair", "Fix car air conditioning and cooling system", "❄️",
                "car ac,automobile ac,cooling,car air conditioner,repair", "car ac repair,automobile ac fix,car cooling", 1, 60, 180, 500, 3000, "car_mechanic,ac_technician"),
            ServiceEntity("car_wash", "vehicle", "Car Wash", "Exterior and interior car wash and cleaning", "🚿",
                "car wash,auto wash,car clean,detailing,exterior,interior clean", "car wash,car cleaning,auto wash,car detailing", 1, 30, 90, 200, 800, "car_wash"),
            ServiceEntity("car_detailing", "vehicle", "Car Detailing", "Premium car detailing and ceramic coating", "✨",
                "car detailing,ceramic coating,polish,wax,car shine,paint correction", "car detailing,ceramic coat,car polish,paint protection", 1, 180, 480, 2000, 10000, "car_detailing"),
            ServiceEntity("tyre_puncture", "vehicle", "Tyre Puncture Repair", "Fix flat tyres and puncture repairs", "🔧",
                "tyre,puncture,flat tyre,wheel,tyre repair,car tyre,bike tyre", "tyre puncture,flat tyre fix,puncture repair,tyre service", 1, 20, 60, 100, 300, "tyre_technician"),
            ServiceEntity("tyre_change", "vehicle", "Tyre Change & Balancing", "Change tyres and wheel balancing & alignment", "⚖️",
                "tyre change,balancing,alignment,wheel,new tyre,rotation", "tyre change,wheel balancing,alignment,tyre rotation", 1, 60, 120, 500, 2000, "tyre_technician"),
            ServiceEntity("car_battery", "vehicle", "Car Battery Service", "Test, jump-start or replace car batteries", "🔋",
                "car battery,battery change,dead battery,jump start,battery replacement", "car battery,battery service,dead car,jump start", 1, 30, 90, 500, 3000, "car_mechanic"),
            ServiceEntity("bike_repair_general", "vehicle", "Bike General Repair", "General motorcycle repair and servicing", "🏍️",
                "bike repair,motorcycle,two wheeler,mechanic,bike service,engine", "bike repair,motorcycle repair,bike mechanic,two wheeler service", 1, 60, 240, 300, 2000, "bike_mechanic"),
            ServiceEntity("bike_service", "vehicle", "Bike Service", "Regular bike servicing and oil change", "🔩",
                "bike service,motorcycle service,oil change,two wheeler,regular service", "bike service,motorcycle service,oil change,tune up", 1, 60, 120, 300, 1000, "bike_mechanic"),
            ServiceEntity("towing", "vehicle", "Vehicle Towing", "Emergency vehicle towing service", "🚛",
                "towing,breakdown,car tow,vehicle transport,emergency,road assistance", "car towing,vehicle towing,tow truck,breakdown towing", 1, 30, 90, 500, 2000, "towing"),
            ServiceEntity("roadside_assistance", "vehicle", "Roadside Assistance", "Emergency roadside help for vehicle breakdowns", "🆘",
                "roadside,breakdown,stuck,car not starting,emergency,help on road", "roadside assistance,breakdown help,emergency car help,car stuck", 1, 20, 90, 300, 1000, "roadside_assistance"),
        ))

        // ═══════════════════════════════
        // TECHNOLOGY
        // ═══════════════════════════════
        services.addAll(listOf(
            ServiceEntity("computer_repair", "technology", "Computer Repair", "Diagnose and fix desktop and laptop hardware issues", "🖥️",
                "computer repair,desktop,cpu,hardware,repair,computer not working", "computer repair,desktop fix,pc repair,computer service", 1, 60, 240, 300, 2000, "computer_technician"),
            ServiceEntity("laptop_repair", "technology", "Laptop Repair", "Fix laptop screens, keyboards, hinges and hardware", "💻",
                "laptop repair,screen,keyboard,hinge,battery,charging,laptop service", "laptop repair,laptop screen fix,keyboard repair,laptop service", 1, 60, 240, 400, 3000, "computer_technician"),
            ServiceEntity("mobile_repair", "technology", "Mobile Phone Repair", "Fix smartphone screens, batteries and hardware", "📱",
                "mobile repair,phone screen,battery,charging,smartphone,phone fix", "mobile repair,phone screen fix,battery replacement,phone service", 1, 30, 90, 300, 3000, "mobile_technician"),
            ServiceEntity("printer_repair", "technology", "Printer Repair", "Fix printers, scanners and multifunction devices", "🖨️",
                "printer repair,scanner,print,cartridge,ink,printer service", "printer repair,printer fix,printer service,cartridge issue", 1, 30, 90, 300, 1500, "computer_technician"),
            ServiceEntity("networking", "technology", "Network & WiFi Setup", "Setup home/office networks, routers and WiFi", "📡",
                "network,wifi,router,internet,networking,broadband,setup,configure", "network setup,wifi configure,router setup,internet setup", 1, 60, 180, 400, 2000, "network_technician"),
            ServiceEntity("data_recovery", "technology", "Data Recovery", "Recover lost data from drives and devices", "💾",
                "data recovery,hard drive,data lost,recover files,hdd,ssd,pendrive", "data recovery,file recovery,hard drive recovery,data restore", 1, 120, 480, 1000, 5000, "data_recovery_specialist"),
            ServiceEntity("software_installation", "technology", "Software Installation", "Install OS, software and configure systems", "⚙️",
                "software install,windows,os,antivirus,format,setup,configure", "software installation,windows install,os setup,antivirus install", 1, 60, 180, 300, 1000, "computer_technician"),
            ServiceEntity("cctv_repair", "technology", "CCTV & Security Repair", "Repair CCTV cameras and security systems", "🎥",
                "cctv repair,camera fix,security system,dvr,nvr,surveillance repair", "cctv repair,security camera fix,dvr repair,surveillance repair", 1, 60, 180, 500, 2000, "cctv_technician"),
        ))

        // ═══════════════════════════════
        // BEAUTY & WELLNESS
        // ═══════════════════════════════
        services.addAll(listOf(
            ServiceEntity("salon_haircut", "beauty_wellness", "Haircut & Styling", "Professional haircut and styling at home", "💇",
                "haircut,hair styling,hair salon,barber,hair trim,home salon", "haircut home,hair styling,salon at home,barber home visit", 1, 30, 90, 200, 800, "stylist"),
            ServiceEntity("salon_facial", "beauty_wellness", "Facial Treatment", "Professional facial and skincare treatments", "✨",
                "facial,skin care,face treatment,glow,beauty,clean up,bleach", "facial at home,skin treatment,face clean up,beauty treatment", 1, 60, 120, 400, 1500, "beautician"),
            ServiceEntity("salon_waxing", "beauty_wellness", "Waxing & Threading", "Body waxing and eyebrow threading", "🪡",
                "waxing,threading,eyebrow,body wax,hair removal,beauty", "waxing at home,threading,eyebrow shaping,hair removal", 1, 30, 90, 200, 800, "beautician"),
            ServiceEntity("makeup_bridal", "beauty_wellness", "Bridal Makeup", "Professional bridal and special occasion makeup", "💄",
                "bridal makeup,wedding makeup,bride,makeup artist,special occasion", "bridal makeup,wedding makeup,makeup artist,bride makeup", 1, 120, 240, 2000, 10000, "makeup_artist"),
            ServiceEntity("makeup_party", "beauty_wellness", "Party Makeup", "Professional makeup for parties and events", "💋",
                "party makeup,occasion makeup,makeup artist,glam,event makeup", "party makeup,event makeup,occasion makeup,makeup home visit", 1, 60, 120, 800, 3000, "makeup_artist"),
            ServiceEntity("spa_massage", "beauty_wellness", "Massage & Spa", "Relaxation and therapeutic massage at home", "🧖",
                "massage,spa,relaxation,therapy,body massage,stress relief", "massage at home,spa home,body massage,relaxation massage", 1, 60, 120, 500, 2000, "spa_therapist"),
            ServiceEntity("fitness_training", "beauty_wellness", "Personal Fitness Training", "One-on-one fitness and workout coaching", "🏋️",
                "personal trainer,fitness,workout,gym trainer,exercise,weight loss", "personal trainer,fitness coaching,workout trainer,gym at home", 1, 60, 90, 500, 2000, "personal_trainer"),
            ServiceEntity("yoga_instructor", "beauty_wellness", "Yoga Classes", "Personal yoga classes and instruction", "🧘",
                "yoga,instructor,meditation,class,flexibility,yoga teacher", "yoga class,yoga instructor,yoga at home,meditation class", 1, 60, 90, 400, 1500, "yoga_instructor"),
        ))

        // ═══════════════════════════════
        // HEALTHCARE
        // ═══════════════════════════════
        services.addAll(listOf(
            ServiceEntity("home_nursing", "healthcare", "Home Nursing", "Professional nursing care at home", "👩‍⚕️",
                "nurse,nursing,home care,patient care,medical,hospital at home", "home nursing,nurse at home,patient care,medical care home", 1, 60, 480, 800, 3000, "nurse"),
            ServiceEntity("physiotherapy", "healthcare", "Physiotherapy at Home", "Physical therapy and rehabilitation at home", "🏃",
                "physiotherapy,physical therapy,rehab,exercise,pain,injury,back pain", "physiotherapy home,physical therapy,rehabilitation,pain relief", 1, 60, 90, 600, 1500, "physiotherapist"),
            ServiceEntity("doctor_visit", "healthcare", "Doctor Home Visit", "Physician consultation at home", "🩺",
                "doctor,physician,home visit,consultation,medicine,sick,fever,illness", "doctor home visit,physician consultation,doctor at home", 1, 30, 60, 500, 1500, "doctor"),
        ))

        // ═══════════════════════════════
        // EDUCATION
        // ═══════════════════════════════
        services.addAll(listOf(
            ServiceEntity("tutoring_school", "education", "School Tutoring", "Home tutoring for school students", "📖",
                "tutor,tutoring,school,student,home tuition,teaching,maths,science,english", "home tuition,school tutor,private tuition,tutoring home", 1, 60, 90, 300, 1000, "tutor"),
            ServiceEntity("tutoring_competitive", "education", "Competitive Exam Coaching", "Coaching for JEE, NEET, UPSC and other exams", "🎯",
                "jee,neet,upsc,coaching,competitive,exam,preparation,tutor", "jee coaching,neet tutor,competitive exam prep,exam coaching", 1, 60, 90, 500, 2000, "tutor"),
            ServiceEntity("music_class", "education", "Music Classes", "Learn guitar, keyboard, vocals and more", "🎸",
                "music,guitar,keyboard,piano,violin,vocal,singing,class,learn", "music class,guitar class,keyboard lessons,singing class", 1, 45, 60, 400, 1200, "music_teacher"),
            ServiceEntity("dance_class", "education", "Dance Classes", "Classical, western and folk dance instruction", "💃",
                "dance,class,bharatanatyam,bollywood,western,hip hop,zumba,learn", "dance class,dance teacher,dance lessons,dance coaching", 1, 60, 90, 300, 1000, "dance_teacher"),
        ))

        // ═══════════════════════════════
        // EVENTS
        // ═══════════════════════════════
        services.addAll(listOf(
            ServiceEntity("photography", "events", "Photography", "Professional photography for events and portraits", "📸",
                "photography,photographer,event,wedding,portrait,photo shoot", "photography,photographer,photo shoot,event photography", 1, 120, 480, 2000, 15000, "photographer"),
            ServiceEntity("videography", "events", "Videography", "Professional video recording and editing", "🎬",
                "videography,videographer,video,wedding,event video,cinematography", "videography,video recording,event video,wedding video", 1, 120, 480, 3000, 20000, "videographer"),
            ServiceEntity("catering", "events", "Catering Services", "Food catering for events and parties", "🍽️",
                "catering,food,party,event,cook,chef,buffet,meals,corporate", "catering service,food catering,party food,event catering", 3, 480, 1440, 5000, 50000, "caterer"),
            ServiceEntity("event_decoration", "events", "Event Decoration", "Decorate venues for birthdays, weddings and events", "🎊",
                "decoration,event decor,birthday,wedding,venue,flowers,balloons", "event decoration,party decor,venue decoration,birthday decoration", 2, 240, 480, 3000, 20000, "decorator"),
            ServiceEntity("dj_music", "events", "DJ & Sound System", "DJ and sound system for events and parties", "🎵",
                "dj,sound,music,party,event,speakers,mixer,disc jockey", "dj hire,sound system,party dj,event music,dj booking", 1, 240, 480, 5000, 25000, "dj"),
        ))

        // ═══════════════════════════════
        // PROFESSIONAL SERVICES
        // ═══════════════════════════════
        services.addAll(listOf(
            ServiceEntity("legal_services", "professional", "Legal Consultation", "Consult lawyers for legal advice and documents", "⚖️",
                "legal,lawyer,advocate,law,consultation,legal advice,document", "legal advice,lawyer consultation,legal help,legal services", 1, 60, 120, 1000, 5000, "lawyer"),
            ServiceEntity("accounting_tax", "professional", "Accounting & Tax Filing", "Income tax filing, bookkeeping and accounting", "📊",
                "accounting,tax,itr,income tax,gst,bookkeeping,ca,chartered accountant", "tax filing,itr filing,gst filing,accounting,tax consultant", 1, 120, 480, 500, 5000, "accountant"),
            ServiceEntity("real_estate", "professional", "Real Estate Services", "Property buying, selling and rental assistance", "🏘️",
                "real estate,property,buy,sell,rent,agent,broker,flat,house,plot", "real estate agent,property dealer,house for rent,flat for sale", 1, 60, 240, 0, 0, "real_estate_agent"),
            ServiceEntity("travel_agent", "professional", "Travel Planning", "Holiday packages, flights and hotel booking", "✈️",
                "travel,tour,holiday,package,flight,hotel,booking,trip,vacation", "travel agent,tour package,holiday planning,flight booking", 1, 60, 120, 0, 0, "travel_agent"),
            ServiceEntity("insurance_agent", "professional", "Insurance Services", "Insurance plans for health, vehicle and life", "🛡️",
                "insurance,health insurance,life insurance,car insurance,policy", "insurance agent,insurance plans,health insurance,life insurance", 1, 60, 120, 0, 0, "insurance_agent"),
        ))

        // ═══════════════════════════════
        // LOGISTICS & DELIVERY
        // ═══════════════════════════════
        services.addAll(listOf(
            ServiceEntity("packers_movers", "logistics", "Packers & Movers", "Home shifting and goods transportation", "📦",
                "packers,movers,shifting,relocation,transport,moving house", "packers movers,house shifting,home relocation,goods transport", 4, 480, 1440, 3000, 20000, "mover"),
            ServiceEntity("courier_delivery", "logistics", "Courier & Delivery", "Local parcel and document pickup and delivery", "🛵",
                "courier,delivery,parcel,pickup,send,document,local delivery", "courier service,local delivery,parcel delivery,pickup drop", 1, 30, 120, 100, 500, "delivery"),
            ServiceEntity("grocery_delivery", "logistics", "Grocery Delivery", "Fresh grocery and essentials doorstep delivery", "🛒",
                "grocery,vegetables,fruits,delivery,home delivery,supermarket", "grocery delivery,vegetable delivery,home grocery,essentials delivery", 1, 30, 90, 0, 0, "delivery"),
        ))

        // ═══════════════════════════════
        // CONSTRUCTION
        // ═══════════════════════════════
        services.addAll(listOf(
            ServiceEntity("construction_tiling", "construction", "Tiling Work", "Floor and wall tile installation and repair", "🏗️",
                "tiling,tiles,floor,wall,bathroom,kitchen tile,install,repair", "tiling work,floor tiling,wall tiles,tile installation", 2, 480, 1440, 2000, 10000, "mason"),
            ServiceEntity("construction_waterproofing", "construction", "Waterproofing", "Roof and wall waterproofing treatment", "🌧️",
                "waterproofing,leak,roof leak,water seepage,water damage,damp wall", "waterproofing,roof waterproofing,water leakage,seepage treatment", 2, 240, 960, 3000, 15000, "mason"),
            ServiceEntity("construction_civil", "construction", "Civil Construction Work", "General construction, renovation and remodeling", "🏚️",
                "construction,civil,renovation,remodel,building,contractor", "civil work,construction,home renovation,building contractor", 4, 960, 14400, 20000, 500000, "contractor"),
            ServiceEntity("plaster_repair", "construction", "Plaster & POP Work", "Wall plastering, POP ceiling and gypsum work", "🪣",
                "plaster,pop,gypsum,ceiling,wall,putty,finish,skim coat", "plaster work,pop ceiling,gypsum work,wall plaster", 2, 240, 1440, 2000, 15000, "mason"),
        ))

        // ═══════════════════════════════
        // PET SERVICES
        // ═══════════════════════════════
        services.addAll(listOf(
            ServiceEntity("pet_grooming", "pet", "Pet Grooming", "Bath, trim and grooming for dogs and cats", "🐕",
                "pet grooming,dog bath,cat bath,pet trim,grooming,pet wash", "dog grooming,cat grooming,pet bath,pet wash,grooming at home", 1, 60, 120, 400, 1500, "pet_groomer"),
            ServiceEntity("pet_care", "pet", "Pet Care & Sitting", "Pet sitting, walking and daily care", "🐾",
                "pet sitting,dog walking,pet care,cat sitting,pet boarding", "pet sitting,dog walking,pet care,cat sitting", 1, 60, 480, 300, 1000, "pet_sitter"),
            ServiceEntity("veterinary_visit", "pet", "Veterinary Home Visit", "Vet consultation and treatment at home", "🐾",
                "vet,veterinary,doctor,pet,dog,cat,animal health,vaccination", "vet home visit,veterinary consultation,pet doctor,animal doctor", 1, 30, 60, 500, 2000, "veterinarian"),
        ))

        // ═══════════════════════════════
        // AGRICULTURE
        // ═══════════════════════════════
        services.addAll(listOf(
            ServiceEntity("agriculture_spraying", "agriculture", "Crop Spraying", "Pesticide and fertilizer spraying for crops", "🌾",
                "farming,crop,spray,pesticide,fertilizer,agriculture,field", "crop spraying,pesticide spray,farm spraying,agriculture services", 1, 120, 480, 500, 3000, "farmer"),
            ServiceEntity("agriculture_equipment", "agriculture", "Farm Equipment Repair", "Repair tractors, pumps and farm machinery", "🚜",
                "tractor,pump,equipment,repair,farm machinery,agricultural", "tractor repair,farm equipment,pump repair,agricultural machinery", 1, 120, 360, 500, 5000, "mechanic"),
        ))

        // ═══════════════════════════════
        // MOVING & CONSTRUCTION extras
        // ═══════════════════════════════
        services.addAll(listOf(
            ServiceEntity("false_ceiling", "construction", "False Ceiling Work", "POP, gypsum and grid false ceiling installation", "🏠",
                "false ceiling,pop ceiling,gypsum,interior,ceiling design,grid ceiling", "false ceiling,gypsum ceiling,pop ceiling,ceiling work", 2, 480, 1440, 3000, 15000, "interior_contractor"),
            ServiceEntity("glass_repair", "home_services", "Glass & Window Repair", "Replace broken window glass and glazing", "🪟",
                "glass,window,broken glass,replace,glazing,door glass", "glass repair,window glass,broken window,glass replacement", 1, 60, 180, 300, 2000, "glazier"),
            ServiceEntity("gate_grill_welding", "construction", "Gate, Grill & Welding", "Fabricate and install metal gates, grills and railings", "🔩",
                "gate,grill,railing,welding,metal,fabrication,iron,steel", "metal gate,grill fabrication,welding work,steel grill,gate repair", 2, 240, 960, 2000, 15000, "welder"),
            ServiceEntity("floor_polishing", "home_services", "Floor Polishing", "Marble, granite and floor polishing and buffing", "✨",
                "floor polish,marble polish,granite,shine,buffing,floor cleaning", "floor polishing,marble polishing,floor buffing,granite polish", 2, 240, 480, 2000, 8000, "floor_polisher"),
            ServiceEntity("water_tank_cleaning", "home_services", "Water Tank Cleaning", "Clean and disinfect overhead and underground water tanks", "🪣",
                "water tank,cleaning,overhead tank,sump,disinfect,tank wash", "water tank cleaning,overhead tank clean,sump cleaning", 1, 120, 240, 500, 2000, "cleaning"),
        ))

        serviceDao.insertAll(services)
    }

    private suspend fun seedProviders() {
        if (providerDao.count() <= 2) {
            val mockProviders = listOf(
                com.smartmaps.app.data.local.entity.ProviderEntity(
                    id = "vendor_ac_1",
                    name = "Rajesh Kumar",
                    businessName = "CoolBreeze AC & Appliance Pro",
                    phone = "+919848011223",
                    email = "rajesh@coolbreeze.in",
                    description = "Certified LG, Samsung & Daikin master technician. Fast AC gas refill, PCB repair, jet service & refrigerator repair with 90-day warranty.",
                    latitude = 17.3910,
                    longitude = 78.4917,
                    address = "Hitech City Main Rd, Near Metro",
                    serviceRadius = 18.0,
                    rating = 4.9f,
                    reviewCount = 164,
                    completedJobs = 412,
                    isVerified = true,
                    priceRangeMin = 350,
                    priceRangeMax = 1200,
                    experienceYears = 11
                ),
                com.smartmaps.app.data.local.entity.ProviderEntity(
                    id = "vendor_plumb_1",
                    name = "Mohammed Irfan",
                    businessName = "AquaFix 24x7 Plumbing & Motors",
                    phone = "+919848022334",
                    email = "irfan@aquafix.in",
                    description = "Emergency leak repair, concealed pipe fixing, pressure pump installation, drain unblocking & Jaguar/Hindware sanitary fittings.",
                    latitude = 17.3800,
                    longitude = 78.4937,
                    address = "Madhapur Phase 2",
                    serviceRadius = 15.0,
                    rating = 4.8f,
                    reviewCount = 128,
                    completedJobs = 350,
                    isVerified = true,
                    priceRangeMin = 200,
                    priceRangeMax = 700,
                    experienceYears = 9
                ),
                com.smartmaps.app.data.local.entity.ProviderEntity(
                    id = "vendor_elec_1",
                    name = "Srinivas Rao",
                    businessName = "VoltGuard Electrical & Inverter Hub",
                    phone = "+919848033445",
                    email = "srinivas@voltguard.in",
                    description = "Licensed A-grade electrician. 20-min emergency response for power trips, MCB faults, inverter/battery setup, ceiling fans & full wiring.",
                    latitude = 17.3890,
                    longitude = 78.4807,
                    address = "Jubilee Hills Checkpost",
                    serviceRadius = 16.0,
                    rating = 4.9f,
                    reviewCount = 210,
                    completedJobs = 530,
                    isVerified = true,
                    priceRangeMin = 150,
                    priceRangeMax = 650,
                    experienceYears = 14
                ),
                com.smartmaps.app.data.local.entity.ProviderEntity(
                    id = "vendor_plumb_2",
                    name = "Venkatesh Goud",
                    businessName = "Sri Sai Rapid Plumbers & Geyser Fix",
                    phone = "+919848044556",
                    email = "venkatesh@srisaiplumb.in",
                    description = "Affordable tap repair, water tank overflow fix, geyser heating element replacement & kitchen sink block clearance.",
                    latitude = 17.3960,
                    longitude = 78.4827,
                    address = "Gachibowli Crossroads",
                    serviceRadius = 14.0,
                    rating = 4.6f,
                    reviewCount = 89,
                    completedJobs = 215,
                    isVerified = true,
                    priceRangeMin = 150,
                    priceRangeMax = 500,
                    experienceYears = 7
                ),
                com.smartmaps.app.data.local.entity.ProviderEntity(
                    id = "vendor_ac_2",
                    name = "Karthik Reddy",
                    businessName = "ArcticCare Home Appliances & AC",
                    phone = "+919848055667",
                    email = "karthik@arcticcare.in",
                    description = "Specialist in inverter split ACs, Bosch/IFB front-load washing machines, double-door refrigerators & RO water purifiers.",
                    latitude = 17.3770,
                    longitude = 78.4797,
                    address = "Kondapur Main Road",
                    serviceRadius = 20.0,
                    rating = 4.7f,
                    reviewCount = 112,
                    completedJobs = 290,
                    isVerified = true,
                    priceRangeMin = 300,
                    priceRangeMax = 1000,
                    experienceYears = 8
                ),
                com.smartmaps.app.data.local.entity.ProviderEntity(
                    id = "vendor_mech_1",
                    name = "Arjun Verma",
                    businessName = "SpeedRescue 24x7 Mechanic & Towing",
                    phone = "+919848066778",
                    email = "arjun@speedrescue.in",
                    description = "On-spot car & bike breakdown repair, tubeless puncture fix, Amaron/Exide battery jumpstart & replacement, flatbed towing.",
                    latitude = 17.3880,
                    longitude = 78.4957,
                    address = "Banjara Hills Road No 12",
                    serviceRadius = 25.0,
                    rating = 4.8f,
                    reviewCount = 175,
                    completedJobs = 480,
                    isVerified = true,
                    priceRangeMin = 200,
                    priceRangeMax = 1200,
                    experienceYears = 10
                ),
                com.smartmaps.app.data.local.entity.ProviderEntity(
                    id = "vendor_carp_1",
                    name = "Narasimha Chary",
                    businessName = "MasterCraft Woods & Locksmith",
                    phone = "+919848077889",
                    email = "chary@mastercraft.in",
                    description = "Expert in smart digital door locks, Godrej lock repair, hydraulic bed repair, modular kitchen hinges & custom wardrobes.",
                    latitude = 17.3760,
                    longitude = 78.4897,
                    address = "Kukatpally Housing Board",
                    serviceRadius = 18.0,
                    rating = 4.7f,
                    reviewCount = 94,
                    completedJobs = 260,
                    isVerified = true,
                    priceRangeMin = 250,
                    priceRangeMax = 900,
                    experienceYears = 12
                ),
                com.smartmaps.app.data.local.entity.ProviderEntity(
                    id = "vendor_clean_1",
                    name = "Priya Nair",
                    businessName = "SparkleNest Deep Clean & Pest Shield",
                    phone = "+919848088990",
                    email = "priya@sparklenest.in",
                    description = "Eco-friendly full home deep cleaning, sofa & mattress steam sanitization, odorless herbal cockroach & termite treatment.",
                    latitude = 17.3920,
                    longitude = 78.4887,
                    address = "Ameerpet Metro Enclave",
                    serviceRadius = 22.0,
                    rating = 4.9f,
                    reviewCount = 143,
                    completedJobs = 390,
                    isVerified = true,
                    priceRangeMin = 499,
                    priceRangeMax = 2499,
                    experienceYears = 6
                ),
                com.smartmaps.app.data.local.entity.ProviderEntity(
                    id = "vendor_tech_1",
                    name = "Vikram Aditya",
                    businessName = "TechPulse Laptop, Mobile & CCTV",
                    phone = "+919848099001",
                    email = "vikram@techpulse.in",
                    description = "Doorstep MacBook/Windows laptop chip-level repair, SSD upgrade, iPhone/Android screen replacement, mesh WiFi & Hikvision CCTV.",
                    latitude = 17.3810,
                    longitude = 78.4817,
                    address = "Begumpet Tech Park",
                    serviceRadius = 20.0,
                    rating = 4.8f,
                    reviewCount = 118,
                    completedJobs = 310,
                    isVerified = true,
                    priceRangeMin = 300,
                    priceRangeMax = 1500,
                    experienceYears = 8
                ),
                com.smartmaps.app.data.local.entity.ProviderEntity(
                    id = "vendor_salon_1",
                    name = "Ananya Sharma",
                    businessName = "GlowAura Home Salon & Bridal Studio",
                    phone = "+919848010112",
                    email = "ananya@glowaura.in",
                    description = "Luxury salon at home — Korean glass facial, Rica waxing, hair spa, keratin, bridal HD makeup & certified therapeutic spa.",
                    latitude = 17.3900,
                    longitude = 78.4787,
                    address = "Jubilee Hills Road 36",
                    serviceRadius = 18.0,
                    rating = 4.9f,
                    reviewCount = 186,
                    completedJobs = 440,
                    isVerified = true,
                    priceRangeMin = 399,
                    priceRangeMax = 2500,
                    experienceYears = 9
                )
            )
            providerDao.insertAll(mockProviders)
            val links = listOf(
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_ac_1", serviceId = "ac_repair"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_ac_1", serviceId = "ac_gas_check"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_ac_1", serviceId = "ac_cleaning"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_ac_1", serviceId = "refrigerator_repair"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_plumb_1", serviceId = "plumbing_tap_repair"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_plumb_1", serviceId = "plumbing_pipe_repair"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_plumb_1", serviceId = "plumbing_drain_cleaning"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_plumb_1", serviceId = "plumbing_toilet_repair"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_elec_1", serviceId = "electrical_wiring"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_elec_1", serviceId = "electrical_fan_repair"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_elec_1", serviceId = "electrical_switch_repair"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_elec_1", serviceId = "electrical_fault_detection"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_elec_1", serviceId = "inverter_battery"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_plumb_2", serviceId = "plumbing_tap_repair"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_plumb_2", serviceId = "plumbing_water_heater"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_ac_2", serviceId = "ac_repair"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_ac_2", serviceId = "washing_machine_repair"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_ac_2", serviceId = "ro_water_purifier"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_mech_1", serviceId = "car_repair_general"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_mech_1", serviceId = "tyre_puncture"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_mech_1", serviceId = "car_battery"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_mech_1", serviceId = "roadside_assistance"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_carp_1", serviceId = "carpentry_furniture"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_carp_1", serviceId = "carpentry_door_window"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_clean_1", serviceId = "cleaning_home"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_clean_1", serviceId = "cleaning_sofa"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_clean_1", serviceId = "pest_control_cockroach"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_tech_1", serviceId = "laptop_repair"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_tech_1", serviceId = "mobile_repair"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_tech_1", serviceId = "cctv_installation"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_salon_1", serviceId = "salon_haircut"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_salon_1", serviceId = "salon_facial"),
                com.smartmaps.app.data.local.entity.ProviderServiceEntity(providerId = "vendor_salon_1", serviceId = "makeup_bridal")
            )
            providerServiceDao.insertAll(links)
        }
    }
}

