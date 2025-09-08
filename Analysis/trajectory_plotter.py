import pandas as pd
import folium
import branca.colormap as cm

df = pd.read_csv("Data/canopus_dataV2.csv")

# df = df.sort_values("timestamp")

coords = list(zip(df["latitude"], df["longitude"]))
rsrp_values = df["rsrp"].values

colormap = cm.linear.RdYlGn_11.scale(rsrp_values.min(), rsrp_values.max())

m = folium.Map(location=coords[0], zoom_start=15)

for (lat, lon, rsrp) in zip(df["latitude"], df["longitude"], rsrp_values):
    folium.CircleMarker(
        location=(lat, lon),
        radius=4,
        color=colormap(rsrp),
        fill=True,
        fill_opacity=0.8,
        popup=f"RSRP: {rsrp} dBm"
    ).add_to(m)

colormap.caption = "RSRP (dBm)"
colormap.add_to(m)

m.save("trajectory.html")
