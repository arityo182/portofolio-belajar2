import { SiteHeader } from "@/components/landing/SiteHeader";
import { Hero } from "@/components/landing/Hero";
import { StatsBar } from "@/components/landing/StatsBar";
import { About } from "@/components/landing/About";
import { HowItWorks } from "@/components/landing/HowItWorks";
import { Features } from "@/components/landing/Features";
import { Faq } from "@/components/landing/Faq";
import { CtaBanner } from "@/components/landing/CtaBanner";
import { SiteFooter } from "@/components/landing/SiteFooter";

export default function Page() {
  return (
    <div className="font-jakarta bg-white text-landing-navy">
      <SiteHeader />
      <main id="main-content">
        <Hero />
        <StatsBar />
        <About />
        <HowItWorks />
        <Features />
        <Faq />
        <CtaBanner />
      </main>
      <SiteFooter />
    </div>
  );
}
