import type { Metadata } from "next";
import "./globals.css";
export const metadata: Metadata = { title: "UMURAVA | Community Resilience Network", description: "Coordinate essential community resources during disruptions." };
export default function RootLayout({children}:{children:React.ReactNode}) { return <html lang="en"><body>{children}</body></html>; }
