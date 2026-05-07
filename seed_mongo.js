// Run with: mongosh StreamVault seed_mongo.js
// Seeds watch_history and users collections matching Phase 3 schema exactly

db.watch_history.drop();
db.users.drop();
db.content.drop();

// ── watch_history ─────────────────────────────────────────────────────────────
// Fields from Phase 3 schema + country + genre (needed for Pipeline 2)
db.watch_history.insertMany([
  { profile_name:"Layla",  content_title:"Breaking Bad",       episode_title:"Pilot",              watch_date:new Date("2024-11-03"), progress_pct:100, device_type:"Mobile",  completed:true,  country:"UAE",   genre:["Thriller","Crime"]    },
  { profile_name:"Layla",  content_title:"Breaking Bad",       episode_title:"Cats in the Bag",    watch_date:new Date("2024-11-05"), progress_pct:100, device_type:"Mobile",  completed:true,  country:"UAE",   genre:["Thriller","Crime"]    },
  { profile_name:"Layla",  content_title:"Inception",          episode_title:null,                 watch_date:new Date("2024-11-10"), progress_pct:100, device_type:"Tablet",  completed:true,  country:"UAE",   genre:["Sci-Fi","Thriller"]   },
  { profile_name:"Ahmed",  content_title:"Inception",          episode_title:null,                 watch_date:new Date("2024-11-12"), progress_pct:100, device_type:"TV",      completed:true,  country:"KSA",   genre:["Sci-Fi","Thriller"]   },
  { profile_name:"Ahmed",  content_title:"Breaking Bad",       episode_title:"Pilot",              watch_date:new Date("2024-11-14"), progress_pct:100, device_type:"TV",      completed:true,  country:"KSA",   genre:["Thriller","Crime"]    },
  { profile_name:"Sara",   content_title:"Interstellar",       episode_title:null,                 watch_date:new Date("2024-11-15"), progress_pct:100, device_type:"Mobile",  completed:true,  country:"Egypt", genre:["Sci-Fi","Drama"]      },
  { profile_name:"Sara",   content_title:"Interstellar",       episode_title:null,                 watch_date:new Date("2024-11-16"), progress_pct:100, device_type:"Mobile",  completed:true,  country:"Egypt", genre:["Sci-Fi","Drama"]      },
  { profile_name:"Mona",   content_title:"The Dark Knight",    episode_title:null,                 watch_date:new Date("2024-11-17"), progress_pct:100, device_type:"Laptop",  completed:true,  country:"UAE",   genre:["Action","Crime"]      },
  { profile_name:"Mona",   content_title:"The Dark Knight",    episode_title:null,                 watch_date:new Date("2024-11-18"), progress_pct:100, device_type:"Laptop",  completed:true,  country:"UAE",   genre:["Action","Crime"]      },
  { profile_name:"Omar",   content_title:"Stranger Things",    episode_title:"Chapter One",        watch_date:new Date("2024-11-19"), progress_pct:100, device_type:"TV",      completed:true,  country:"Jordan",genre:["Horror","Sci-Fi"]     },
  { profile_name:"Omar",   content_title:"Stranger Things",    episode_title:"Chapter Two",        watch_date:new Date("2024-11-20"), progress_pct:100, device_type:"TV",      completed:true,  country:"Jordan",genre:["Horror","Sci-Fi"]     },
  { profile_name:"Nora",   content_title:"The Crown",          episode_title:"Wolferton Splash",   watch_date:new Date("2024-11-21"), progress_pct:100, device_type:"Tablet",  completed:true,  country:"KSA",   genre:["Drama","History"]     },
  { profile_name:"Nora",   content_title:"Avengers Endgame",   episode_title:null,                 watch_date:new Date("2024-11-22"), progress_pct:80,  device_type:"Tablet",  completed:false, country:"KSA",   genre:["Action","Sci-Fi"]     },
  { profile_name:"Layla",  content_title:"Ozark",              episode_title:"Sugarwood",          watch_date:new Date("2024-11-23"), progress_pct:50,  device_type:"Mobile",  completed:false, country:"UAE",   genre:["Thriller","Drama"]    },
  { profile_name:"Ahmed",  content_title:"Inception",          episode_title:null,                 watch_date:new Date("2024-11-24"), progress_pct:100, device_type:"Laptop",  completed:true,  country:"KSA",   genre:["Sci-Fi","Thriller"]   },
  { profile_name:"Sara",   content_title:"Breaking Bad",       episode_title:"Pilot",              watch_date:new Date("2024-11-25"), progress_pct:100, device_type:"TV",      completed:true,  country:"Egypt", genre:["Thriller","Crime"]    },
  { profile_name:"Zara",   content_title:"Better Call Saul",   episode_title:"Uno",                watch_date:new Date("2024-11-26"), progress_pct:100, device_type:"TV",      completed:true,  country:"UAE",   genre:["Drama","Crime"]       },
  { profile_name:"Zara",   content_title:"Better Call Saul",   episode_title:"Mijo",               watch_date:new Date("2024-11-27"), progress_pct:100, device_type:"TV",      completed:true,  country:"UAE",   genre:["Drama","Crime"]       },
  { profile_name:"Khalid", content_title:"The Witcher",        episode_title:"The End's Beginning", watch_date:new Date("2024-11-28"), progress_pct:60, device_type:"Mobile",  completed:false, country:"Jordan",genre:["Fantasy","Action"]    },
  { profile_name:"Hana",   content_title:"Interstellar",       episode_title:null,                 watch_date:new Date("2024-11-29"), progress_pct:100, device_type:"TV",      completed:true,  country:"Egypt", genre:["Sci-Fi","Drama"]      }
]);

// ── users ─────────────────────────────────────────────────────────────────────
// Phase 3 schema + last_active field (needed for Pipeline 3 churn detection)
db.users.insertMany([
  {
    full_name: "Layla Al-Farsi",
    email: "layla.alfarsi@email.com",
    date_of_birth: "1995-03-12",
    country: "UAE",
    join_date: "2024-01-15",
    is_active: true,
    last_active: new Date("2024-11-29"),
    subscription: { plan_name: "Premium", monthly_price: 15.99, status: "active", start_date: "2024-01-15", auto_renew: true },
    profiles: [{ profile_name: "Layla", age_group: "Adult", language_pref: "AR" }],
    devices: [{ device_name: "Layla iPhone 14", device_type: "Mobile", is_trusted: true }]
  },
  {
    full_name: "Ahmed Al-Rashid",
    email: "ahmed.rashid@email.com",
    date_of_birth: "1990-07-22",
    country: "KSA",
    join_date: "2024-02-10",
    is_active: true,
    last_active: new Date("2024-11-24"),
    subscription: { plan_name: "Standard", monthly_price: 9.99, status: "active", start_date: "2024-02-10", auto_renew: true },
    profiles: [{ profile_name: "Ahmed", age_group: "Adult", language_pref: "AR" }],
    devices: [{ device_name: "Ahmed Samsung TV", device_type: "TV", is_trusted: true }]
  },
  {
    full_name: "Sara Hassan",
    email: "sara.hassan@email.com",
    date_of_birth: "1998-11-05",
    country: "Egypt",
    join_date: "2024-03-01",
    is_active: false,
    last_active: new Date("2024-09-01"),
    subscription: { plan_name: "Basic", monthly_price: 4.99, status: "inactive", start_date: "2024-03-01", auto_renew: false },
    profiles: [{ profile_name: "Sara", age_group: "Adult", language_pref: "EN" }],
    devices: [{ device_name: "Sara iPhone", device_type: "Mobile", is_trusted: true }]
  },
  {
    full_name: "Omar Khalil",
    email: "omar.khalil@email.com",
    date_of_birth: "1993-04-18",
    country: "Jordan",
    join_date: "2024-01-20",
    is_active: false,
    last_active: new Date("2024-08-15"),
    subscription: { plan_name: "Premium", monthly_price: 15.99, status: "inactive", start_date: "2024-01-20", auto_renew: false },
    profiles: [{ profile_name: "Omar", age_group: "Adult", language_pref: "EN" }],
    devices: [{ device_name: "Omar TV", device_type: "TV", is_trusted: false }]
  },
  {
    full_name: "Nora Al-Saeed",
    email: "nora.saeed@email.com",
    date_of_birth: "1997-08-30",
    country: "KSA",
    join_date: "2024-04-05",
    is_active: false,
    last_active: new Date("2024-07-20"),
    subscription: { plan_name: "Standard", monthly_price: 9.99, status: "inactive", start_date: "2024-04-05", auto_renew: false },
    profiles: [{ profile_name: "Nora", age_group: "Adult", language_pref: "AR" }, { profile_name: "Kids", age_group: "Child", language_pref: "EN" }],
    devices: [{ device_name: "Nora iPad", device_type: "Tablet", is_trusted: true }]
  },
  {
    full_name: "Zara Ahmed",
    email: "zara.ahmed@email.com",
    date_of_birth: "2000-01-15",
    country: "UAE",
    join_date: "2024-05-10",
    is_active: true,
    last_active: new Date("2024-11-27"),
    subscription: { plan_name: "Premium", monthly_price: 15.99, status: "active", start_date: "2024-05-10", auto_renew: true },
    profiles: [{ profile_name: "Zara", age_group: "Adult", language_pref: "EN" }],
    devices: [{ device_name: "Zara Smart TV", device_type: "TV", is_trusted: true }]
  }
]);

// ── content ───────────────────────────────────────────────────────────────────
db.content.insertMany([
  { title: "Breaking Bad",    type: "Series", release_year: 2008, age_rating: "R",     studio: "AMC",          languages: ["English"],          genres: ["Thriller","Crime"],   average_rating: 9.5, total_reviews: 2, episodes: [{ season_no:1, episode_no:1, title:"Pilot", duration_minutes:58 }, { season_no:1, episode_no:2, title:"Cats in the Bag", duration_minutes:48 }] },
  { title: "Inception",       type: "Movie",  release_year: 2010, age_rating: "PG-13", studio: "Warner Bros.", languages: ["English","French"],  genres: ["Sci-Fi","Thriller"],  average_rating: 8.8, total_reviews: 5, episodes: [] },
  { title: "Interstellar",    type: "Movie",  release_year: 2014, age_rating: "PG-13", studio: "Warner Bros.", languages: ["English"],          genres: ["Sci-Fi","Drama"],     average_rating: 8.6, total_reviews: 4, episodes: [] },
  { title: "The Dark Knight", type: "Movie",  release_year: 2008, age_rating: "PG-13", studio: "Warner Bros.", languages: ["English"],          genres: ["Action","Crime"],     average_rating: 9.0, total_reviews: 6, episodes: [] },
  { title: "Stranger Things", type: "Series", release_year: 2016, age_rating: "TV-14", studio: "Netflix",      languages: ["English"],          genres: ["Horror","Sci-Fi"],    average_rating: 8.7, total_reviews: 3, episodes: [{ season_no:1, episode_no:1, title:"Chapter One", duration_minutes:47 }, { season_no:1, episode_no:2, title:"Chapter Two", duration_minutes:55 }] },
  { title: "The Crown",       type: "Series", release_year: 2016, age_rating: "TV-MA", studio: "Netflix",      languages: ["English"],          genres: ["Drama","History"],    average_rating: 8.7, total_reviews: 2, episodes: [{ season_no:1, episode_no:1, title:"Wolferton Splash", duration_minutes:57 }] },
  { title: "Better Call Saul",type: "Series", release_year: 2015, age_rating: "TV-MA", studio: "AMC",          languages: ["English","Spanish"],genres: ["Drama","Crime"],      average_rating: 9.0, total_reviews: 3, episodes: [{ season_no:1, episode_no:1, title:"Uno", duration_minutes:53 }, { season_no:1, episode_no:2, title:"Mijo", duration_minutes:46 }] },
  { title: "Avengers Endgame",type: "Movie",  release_year: 2019, age_rating: "PG-13", studio: "Marvel",       languages: ["English"],          genres: ["Action","Sci-Fi"],    average_rating: 8.4, total_reviews: 10, episodes: [] },
  { title: "Ozark",           type: "Series", release_year: 2017, age_rating: "TV-MA", studio: "Netflix",      languages: ["English"],          genres: ["Thriller","Drama"],   average_rating: 8.4, total_reviews: 2, episodes: [{ season_no:1, episode_no:1, title:"Sugarwood", duration_minutes:60 }] },
  { title: "The Witcher",     type: "Series", release_year: 2019, age_rating: "TV-MA", studio: "Netflix",      languages: ["English","Polish"], genres: ["Fantasy","Action"],   average_rating: 8.2, total_reviews: 2, episodes: [{ season_no:1, episode_no:1, title:"The End's Beginning", duration_minutes:61 }] }
]);

print("Seed complete:");
print("  watch_history: " + db.watch_history.countDocuments() + " docs");
print("  users: " + db.users.countDocuments() + " docs");
print("  content: " + db.content.countDocuments() + " docs");
