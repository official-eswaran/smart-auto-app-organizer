package com.smartorganizer.launcher.engine

/**
 * Generates a synthetic training corpus for the Naive Bayes classifier.
 *
 * Each entry is a Pair<featureText, category> where featureText = "$appName $packageName".
 * Augmented with realistic app name patterns to improve generalisation.
 */
object TrainingCorpus {

    private val templates = mapOf(
        "Payments" to listOf(
            "GPay com.google.android.apps.nbu.paisa.user",
            "PhonePe com.phonepe.app",
            "Paytm net.one97.paytm",
            "BHIM upi com.upi.bhim",
            "Amazon Pay com.amazon.mShop.android.shopping",
            "Google Pay payments com.google.gpay",
            "PayZapp hdfc bank wallet com.hdfc.payzapp",
            "MobiKwik wallet com.mobikwik_new",
            "Freecharge pay com.freecharge.android",
            "Airtel Thanks payment com.airtel.thanks",
            "iMobile Pay ICICI bank com.csam.icici.bank.imobile",
            "SBI YONO bank com.sbi.lotusintouch",
            "HDFC Bank net banking com.snapwork.hdfc",
            "Kotak bank net com.kotak.mahindra.kotak",
            "Axis Mobile bank com.axis.mobile",
            "Bank of Baroda bob com.bankofbaroda.mobilebanking",
            "UPI money transfer com.example.upipay",
            "Wallet cash com.example.cashwallet",
            "Credit card debit com.example.creditcard",
            "Money transfer finance com.example.moneytransfer"
        ),
        "Games" to listOf(
            "BGMI com.pubg.imobile",
            "Clash of Clans com.supercell.clashofclans",
            "Clash Royale com.supercell.clashroyale",
            "Free Fire battlegrounds com.dts.freefireth",
            "Ludo King game com.ludo.king",
            "Chess com.chess",
            "Candy Crush Saga game com.king.candycrushsaga",
            "Temple Run 2 game com.imangi.templerun2",
            "Subway Surfers runner com.kiloo.subwaysurfers",
            "Among Us game com.innersloth.spacemafia",
            "Call of Duty Mobile shooter com.activision.callofduty.shooter",
            "Roblox game com.roblox.client",
            "My Talking Tom game com.outfit7.mytalkingtomfriends",
            "8 Ball Pool game com.miniclip.eightballpool",
            "CarX Racing race com.carxtech.carxstreetmobile",
            "Pokémon GO game com.nianticlabs.pokemongo",
            "Asphalt racing game com.gameloft.android.ANMP.GloftA9HM",
            "Battle arena com.example.battlearena",
            "Puzzle game adventure com.example.puzzlegame",
            "War craft game com.example.wargame"
        ),
        "Social" to listOf(
            "WhatsApp Messenger com.whatsapp",
            "Telegram com.telegram.messenger",
            "Instagram com.instagram.android",
            "Facebook com.facebook.katana",
            "Snapchat com.snapchat.android",
            "Twitter com.twitter.android",
            "LinkedIn com.linkedin.android",
            "Discord chat com.discord",
            "Signal messenger com.signalapp.advanced",
            "Viber chat com.viber.voip",
            "Hike messenger com.bsb.hike",
            "ShareChat social com.sharechat.sharechat",
            "Moj social video com.sharechat.moj",
            "Josh short video com.newshunt.josh",
            "MX TakaTak social com.mxtakatak",
            "YouTube social share com.google.android.youtube",
            "TikTok social com.zhiliaoapp.musically",
            "Meet connect com.example.socialapp",
            "Chat message com.example.messenger",
            "Social network connect com.example.socialnetwork"
        ),
        "Shopping" to listOf(
            "Amazon Shopping com.amazon.mShop.android.shopping",
            "Flipkart com.flipkart.android",
            "Myntra fashion com.myntra.android",
            "Meesho shopping com.meesho.supply",
            "Ajio fashion com.ril.ajio",
            "Nykaa beauty com.nykaa.app",
            "Snapdeal com.snapdeal.main",
            "JioMart shopping com.jiomart.android",
            "Tata Cliq shopping com.tatacliq",
            "Shopsy flipkart com.shopsy.app",
            "BigBasket grocery com.bigbasket.mobileapp",
            "Blinkit grocery com.grofers.customerapp",
            "Zepto grocery delivery com.zeptonow.app",
            "Swiggy Instamart shop com.bundl.app",
            "Limeroad fashion com.limeroad.shop",
            "Cart store buy com.example.shopstore",
            "Market deal sale com.example.marketapp",
            "Commerce mall com.example.ecommerce",
            "Order delivery shop com.example.orderapp",
            "Fashion buy store com.example.fashionstore"
        ),
        "Music" to listOf(
            "Spotify Music com.spotify.music",
            "Gaana com.gaana",
            "JioSaavn music com.jio.media.jiobeats",
            "Wynk Music com.bsbportal.music",
            "YouTube Music com.google.android.apps.youtube.music",
            "Amazon Music com.amazon.mp3",
            "Apple Music com.apple.android.music",
            "Hungama music com.hungama.myplay",
            "Resso music com.moonvideo.android.resso",
            "SoundCloud music com.soundcloud.android",
            "Podcast app com.example.podcastapp",
            "FM Radio com.example.fmradio",
            "MP3 music player com.example.musicplayer",
            "Audio stream com.example.audiostream",
            "Beat song com.example.beatapp",
            "Tune player song com.example.tuneapp",
            "Rhythm music sound com.example.rhythmapp",
            "Track song audio com.example.trackplayer",
            "Shazam music com.shazam.android",
            "Radio music fm com.example.radioapp"
        ),
        "Health" to listOf(
            "HealthifyMe fitness com.healthifyme.basic",
            "Practo doctor com.practo.patient",
            "PharmEasy pharmacy com.pharmeasy.consumer",
            "1mg pharmacy com.aranoah.healthkart.new",
            "Cult.fit workout com.healthifyme.yoga",
            "Nike Run fitness com.nike.plusgps",
            "Step counter health com.example.stepcounter",
            "Yoga workout com.example.yogaapp",
            "Calorie counter diet com.example.calorieapp",
            "Doctor appointment med com.example.doctorapp",
            "Hospital care com.example.hospitalapp",
            "Blood pressure heart com.example.bpapp",
            "Medlife pharmacy com.medlife",
            "Apollo hospital com.apollopatientapp",
            "Netmeds medicine com.netmeds.marketplace",
            "Diet plan nutrition com.example.dietapp",
            "Pulse oximeter health com.example.pulseapp",
            "Wellness fitness com.example.wellnessapp",
            "Mental health care com.example.mentalhealth",
            "Gym workout fitness com.example.gymapp"
        ),
        "Travel" to listOf(
            "Uber com.ubercab",
            "Ola Cabs com.olacabs.customer",
            "Rapido bike cab com.rapido.passenger",
            "MakeMyTrip travel com.makemytrip",
            "IRCTC Rail Connect train com.centralios.rlvyuttarbharat",
            "Goibibo travel com.ibibo.gozook",
            "Yatra travel com.yatra.base",
            "ixigo trips train com.ixigo.train.production",
            "Cleartrip flight com.cleartrip",
            "Booking hotel com.booking",
            "Airbnb stay hotel com.airbnb.android",
            "OYO hotel com.oyo.consumer.app",
            "Google Maps navigation com.google.android.apps.maps",
            "Ola Electric ride com.olaelectric.consumer",
            "redBus bus ticket com.redbus.india.android",
            "Treebo hotel booking com.treebo.app.consumer",
            "IndiGo flight com.goindigo.android",
            "Air India flight com.airindia.app",
            "Route navigation map com.example.routeapp",
            "Trip travel cab com.example.travelapp"
        ),
        "News" to listOf(
            "Inshorts News com.nis.inshorts",
            "Times of India newspaper com.toi.reader.activities",
            "Hindustan Times news com.htmedia.htapp",
            "NDTV news com.winit.ndtv",
            "ABP Live news com.abp.news",
            "Zee News com.zeenews.android",
            "India Today news com.indiatoday.android",
            "Flipboard news com.flipboard.app",
            "Google News feed com.google.android.apps.magazines",
            "Dailyhunt news com.eterno",
            "BBC News headlines com.bbc.news",
            "CNN news live com.cnn.mobile.android.phone",
            "The Hindu newspaper com.thehindu.thn",
            "Economic Times news com.et.reader.activities",
            "Republic Bharat news com.republic.bharat.android",
            "LiveHindustan news com.livehindustan.app",
            "News daily feed com.example.newsapp",
            "Breaking news headlines com.example.breakingnews",
            "Current affairs update com.example.currentaffairs",
            "Times daily news com.example.timesnews"
        ),
        "Others" to listOf(
            "Calculator com.google.android.calculator",
            "Clock alarm com.google.android.deskclock",
            "Camera photos com.google.android.GoogleCamera",
            "Files manager com.google.android.documentsui",
            "Settings system com.android.settings",
            "Gallery photos com.google.android.apps.photos",
            "Calendar events com.google.android.calendar",
            "Contacts phone com.google.android.contacts",
            "Messages SMS com.google.android.apps.messaging",
            "Phone Dialer com.google.android.dialer",
            "Email app com.google.android.gm",
            "Drive storage com.google.android.apps.docs",
            "Notes memo com.example.notesapp",
            "Tasks todo com.example.tasksapp",
            "PDF reader viewer com.example.pdfreader",
            "QR scanner barcode com.example.qrscanner",
            "Flashlight torch com.example.flashlight",
            "Translator language com.google.android.apps.translate",
            "Weather forecast com.google.android.apps.weather",
            "Random unknown app com.unknown.randomxyz"
        )
    )

    /** Returns the full training corpus as (featureText, category) pairs. */
    fun generate(): List<Pair<String, String>> {
        val corpus = mutableListOf<Pair<String, String>>()
        for ((category, entries) in templates) {
            for (entry in entries) {
                corpus.add(entry to category)
                // Data augmentation: add reversed order and partial text
                val parts = entry.split(" ")
                if (parts.size >= 2) {
                    corpus.add("${parts.last()} ${parts.first()}" to category)
                }
            }
        }
        return corpus.shuffled()
    }
}
