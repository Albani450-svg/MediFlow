import { LitElement, css } from 'lit';
import { customElement } from 'lit/decorators.js';
import { router } from './router';
import './styles/global.css';

@customElement('app-index')
export class AppIndex extends LitElement {
  static styles = css`
    :host {
      display: block;
      min-height: 100%;
    }
  `;

  firstUpdated() {
    router.addEventListener('route-changed', () => {
      if ('startViewTransition' in document) {
        document.startViewTransition(() => this.requestUpdate());
      } else {
        this.requestUpdate();
      }
    });
  }

  render() {
    return router.render();
  }
}
