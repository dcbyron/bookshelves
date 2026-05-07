import { config } from "@vue/test-utils";

config.global.stubs = {
  RouterLink: {
    props: ["to"],
    template: "<a :href=\"typeof to === 'string' ? to : '#' \"><slot /></a>"
  }
};

class ResizeObserverMock {
  observe() {}
  unobserve() {}
  disconnect() {}
}

Object.defineProperty(window, "ResizeObserver", {
  writable: true,
  value: ResizeObserverMock
});

Object.defineProperty(window, "matchMedia", {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener() {},
    removeListener() {},
    addEventListener() {},
    removeEventListener() {},
    dispatchEvent() {
      return false;
    }
  })
});
