<script setup>
import logoMark from "../assets/logoMark.png";

defineEmits(["skip"]);

const particles = Array.from({ length: 32 }, (_, index) => ({
  id: index,
  type: index % 5,
  distance: 118 + ((index * 47) % 220),
  delay: (index % 7) * 18
}));

const brandLetters = [..."Vidayoung"];
</script>

<template>
  <div class="splash" role="status" aria-live="polite" aria-label="Cargando Vidayoung">
    <div class="ambient ambient-one" aria-hidden="true"></div>
    <div class="ambient ambient-two" aria-hidden="true"></div>

    <div class="radial-scene" aria-hidden="true">
      <span class="orbit orbit-one"></span>
      <span class="orbit orbit-two"></span>

      <span
        v-for="particle in particles"
        :key="particle.id"
        class="particle-axis"
        :style="{
          '--angle': `${particle.id * (360 / particles.length)}deg`,
          '--distance': `${particle.distance}px`,
          '--particle-delay': `${particle.delay}ms`
        }"
      >
        <span class="particle" :class="`particle-${particle.type}`">
          <i></i>
        </span>
      </span>
    </div>

    <div class="brand-core">
      <span class="core-halo" aria-hidden="true"></span>
      <span class="core-ring" aria-hidden="true"></span>
      <div class="core-logo">
        <img :src="logoMark" alt="Vidayoung" />
      </div>
      <div class="brand-copy">
        <strong aria-label="Vidayoung">
          <span
            v-for="(letter, index) in brandLetters"
            :key="`${letter}-${index}`"
            class="brand-letter"
            aria-hidden="true"
            :style="{ '--letter-index': index }"
          >{{ letter }}</span>
        </strong>
        <span class="brand-tagline">Generando bienestar</span>
      </div>
    </div>

    <div class="splash-signature" aria-hidden="true">
      <span>Vida</span><i></i><span>Comunidad</span><i></i><span>Bienestar</span>
    </div>

    <button class="skip-button" type="button" @click="$emit('skip')">
      Saltar intro
      <span aria-hidden="true">→</span>
    </button>
  </div>
</template>

<style scoped>
.splash {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: grid;
  place-items: center;
  min-height: 100vh;
  min-height: 100dvh;
  overflow: hidden;
  color: var(--vy-ink);
  background:
    radial-gradient(circle at 50% 48%, rgba(242, 184, 133, 0.16), transparent 25%),
    linear-gradient(145deg, #fffefa 0%, var(--vy-bg) 100%);
  isolation: isolate;
}

.ambient {
  position: absolute;
  z-index: -1;
  border-radius: 50%;
  filter: blur(2px);
  opacity: 0.42;
}

.ambient-one {
  width: min(34vw, 460px);
  aspect-ratio: 1;
  top: -24%;
  right: -8%;
  background: radial-gradient(circle, rgba(242, 135, 5, 0.18), transparent 70%);
}

.ambient-two {
  width: min(30vw, 400px);
  aspect-ratio: 1;
  left: -12%;
  bottom: -20%;
  background: radial-gradient(circle, rgba(31, 26, 20, 0.09), transparent 70%);
}

.radial-scene {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 1px;
  height: 1px;
  animation: scene-finish 2.7s cubic-bezier(0.65, 0, 0.35, 1) both;
}

.orbit {
  position: absolute;
  top: 0;
  left: 0;
  border: 1px solid rgba(242, 135, 5, 0.15);
  border-radius: 50%;
  transform: translate(-50%, -50%);
}

.orbit-one {
  width: 270px;
  height: 270px;
  animation: orbit-breathe 1.8s ease-out both;
}

.orbit-two {
  width: 410px;
  height: 410px;
  border-style: dashed;
  border-color: rgba(31, 26, 20, 0.07);
  animation: orbit-breathe 2s 0.08s ease-out both;
}

.particle-axis {
  --angle: 0deg;
  --distance: 200px;
  --particle-delay: 0ms;
  position: absolute;
  top: 0;
  left: 0;
  width: 1px;
  height: 1px;
  transform: rotate(var(--angle));
}

.particle {
  position: absolute;
  top: 0;
  left: 0;
  display: grid;
  place-items: center;
  transform: translateX(var(--distance)) rotate(calc(-1 * var(--angle)));
  animation: particle-journey 2.55s var(--particle-delay) cubic-bezier(0.72, 0, 0.2, 1) both;
}

.particle i {
  display: block;
  background: currentColor;
}

.particle-0 {
  width: 34px;
  height: 9px;
  color: var(--vy-orange);
}

.particle-0 i {
  width: 100%;
  height: 3px;
  border-radius: 999px;
  box-shadow: 14px 0 16px currentColor;
}

.particle-1 {
  width: 16px;
  height: 16px;
  color: var(--vy-beige);
  border: 2px solid currentColor;
  border-radius: 50%;
}

.particle-1 i {
  width: 4px;
  height: 4px;
  border-radius: 50%;
}

.particle-2 {
  width: 7px;
  height: 42px;
  color: var(--vy-orange-deep);
}

.particle-2 i {
  width: 3px;
  height: 100%;
  border-radius: 999px;
  opacity: 0.78;
}

.particle-3 {
  width: 24px;
  height: 24px;
  color: var(--vy-cream);
  border-radius: 8px;
  background: var(--vy-ink);
  box-shadow: 0 6px 16px rgba(31, 26, 20, 0.16);
}

.particle-3 i {
  width: 7px;
  height: 7px;
  border-radius: 2px;
  background: var(--vy-orange);
}

.particle-4 {
  width: 11px;
  height: 11px;
  color: var(--vy-orange);
}

.particle-4 i {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  box-shadow: 0 0 0 5px rgba(242, 135, 5, 0.1);
}

.brand-core {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  animation: core-finish 2.75s cubic-bezier(0.65, 0, 0.35, 1) both;
}

.core-logo {
  position: relative;
  z-index: 2;
  width: clamp(92px, 9vw, 124px);
  aspect-ratio: 1;
  display: grid;
  place-items: center;
  border: 1px solid rgba(242, 135, 5, 0.22);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.94);
  box-shadow:
    0 18px 50px rgba(31, 26, 20, 0.12),
    0 0 0 10px rgba(242, 135, 5, 0.06);
  backdrop-filter: blur(12px);
  animation: core-arrive 0.75s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.core-logo img {
  width: 54%;
  height: auto;
  filter: drop-shadow(0 7px 10px rgba(242, 135, 5, 0.18));
}

.core-halo,
.core-ring {
  position: absolute;
  top: calc(clamp(92px, 9vw, 124px) / 2);
  left: 50%;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
}

.core-halo {
  width: clamp(150px, 16vw, 220px);
  aspect-ratio: 1;
  background: radial-gradient(circle, rgba(242, 135, 5, 0.15), transparent 68%);
  animation: halo-pulse 1.25s ease-in-out infinite alternate;
}

.core-ring {
  width: clamp(118px, 12vw, 160px);
  aspect-ratio: 1;
  border: 1px solid rgba(242, 135, 5, 0.24);
  animation: ring-wave 1.4s ease-out infinite;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 22px;
  animation: copy-arrive 0.65s 0.28s both;
}

.brand-copy strong {
  display: flex;
  align-items: baseline;
  color: var(--vy-ink);
  font-size: clamp(24px, 2.5vw, 34px);
  font-weight: 850;
  line-height: 1;
  letter-spacing: -0.055em;
  perspective: 400px;
}

.brand-letter {
  display: inline-block;
  transform-origin: center bottom;
  animation: letter-arrive 0.68s calc(0.38s + (var(--letter-index) * 0.055s)) cubic-bezier(0.22, 1, 0.36, 1) both;
}

.brand-letter:nth-child(4),
.brand-letter:nth-child(8) {
  color: var(--vy-orange-deep);
}

.brand-copy > .brand-tagline {
  margin-top: 9px;
  color: var(--vy-orange-deep);
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  animation: tagline-reveal 0.7s 0.88s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.splash-signature {
  position: absolute;
  left: 50%;
  bottom: 30px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--vy-ink-3);
  font-size: 9px;
  font-weight: 750;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  transform: translateX(-50%);
  animation: copy-arrive 0.6s 0.5s both;
}

.splash-signature i {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--vy-orange);
}

.skip-button {
  position: absolute;
  right: 28px;
  bottom: 24px;
  z-index: 4;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  padding: 0 17px;
  border: 1px solid rgba(31, 26, 20, 0.12);
  border-radius: 999px;
  color: var(--vy-ink-2);
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 6px 20px rgba(31, 26, 20, 0.06);
  backdrop-filter: blur(10px);
  font-family: inherit;
  font-size: 11px;
  font-weight: 750;
  cursor: pointer;
  transition: color 0.2s ease, border-color 0.2s ease, transform 0.2s ease;
  animation: copy-arrive 0.6s 0.65s both;
}

.skip-button:hover {
  color: var(--vy-orange-deep);
  border-color: rgba(242, 135, 5, 0.35);
  transform: translateY(-2px);
}

.skip-button:focus-visible {
  outline: 3px solid rgba(242, 135, 5, 0.25);
  outline-offset: 3px;
}

.skip-button span {
  color: var(--vy-orange);
  font-size: 15px;
}

.splash-leave-active {
  transition: opacity 0.42s ease;
}

.splash-leave-to {
  opacity: 0;
}

@keyframes particle-journey {
  0% {
    opacity: 0;
    filter: blur(4px);
    transform: translateX(calc(var(--distance) * 0.28)) rotate(calc(-1 * var(--angle))) scale(0.45);
  }
  18% {
    opacity: 0.95;
    filter: blur(0);
  }
  55% {
    opacity: 0.75;
    transform: translateX(var(--distance)) rotate(calc(-1 * var(--angle))) scale(1);
  }
  72% {
    opacity: 0.75;
    transform: translateX(var(--distance)) rotate(calc(-1 * var(--angle))) scale(1);
  }
  100% {
    opacity: 0;
    filter: blur(3px);
    transform: translateX(16px) rotate(calc(-1 * var(--angle))) scale(0.15);
  }
}

@keyframes core-arrive {
  from { opacity: 0; transform: scale(0.4) rotate(-8deg); }
  to { opacity: 1; transform: scale(1) rotate(0); }
}

@keyframes copy-arrive {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes letter-arrive {
  0% {
    opacity: 0;
    filter: blur(7px);
    transform: translateY(22px) translateX(-7px) rotateX(-75deg) rotateZ(-7deg) scale(0.72);
  }
  65% {
    opacity: 1;
    filter: blur(0);
    transform: translateY(-3px) translateX(0) rotateX(5deg) rotateZ(0) scale(1.04);
  }
  100% {
    opacity: 1;
    filter: blur(0);
    transform: translateY(0) translateX(0) rotateX(0) rotateZ(0) scale(1);
  }
}

@keyframes tagline-reveal {
  from {
    opacity: 0;
    clip-path: inset(0 100% 0 0);
    transform: translateY(7px);
  }
  to {
    opacity: 1;
    clip-path: inset(0 0 0 0);
    transform: translateY(0);
  }
}

@keyframes orbit-breathe {
  from { opacity: 0; transform: translate(-50%, -50%) scale(0.28); }
  to { opacity: 1; transform: translate(-50%, -50%) scale(1); }
}

@keyframes halo-pulse {
  from { opacity: 0.55; transform: translate(-50%, -50%) scale(0.84); }
  to { opacity: 1; transform: translate(-50%, -50%) scale(1.12); }
}

@keyframes ring-wave {
  from { opacity: 0.65; transform: translate(-50%, -50%) scale(0.72); }
  to { opacity: 0; transform: translate(-50%, -50%) scale(1.38); }
}

@keyframes scene-finish {
  0%, 78% { transform: scale(1); opacity: 1; }
  100% { transform: scale(0.16); opacity: 0; }
}

@keyframes core-finish {
  0%, 80% { transform: scale(1); opacity: 1; }
  100% { transform: scale(0.72); opacity: 0; }
}

@media (max-width: 640px) {
  .orbit-one { width: 210px; height: 210px; }
  .orbit-two { width: 320px; height: 320px; }

  .particle-axis {
    --distance: min(var(--distance), 44vw);
  }

  .splash-signature {
    left: 20px;
    bottom: 30px;
    flex-direction: column;
    align-items: flex-start;
    gap: 5px;
    transform: none;
  }

  .splash-signature i {
    display: none;
  }

  .skip-button {
    right: 18px;
    bottom: 22px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .splash *,
  .splash *::before,
  .splash *::after {
    animation-duration: 0.01ms !important;
    animation-delay: 0ms !important;
    animation-iteration-count: 1 !important;
  }

  .splash-leave-active {
    transition-duration: 0.15s;
  }
}
</style>
