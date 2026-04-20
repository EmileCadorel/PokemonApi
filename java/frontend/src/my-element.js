import { LitElement, css, html } from 'lit'
import litLogo from './assets/lit.svg'
import viteLogo from './assets/vite.svg'
import heroImg from './assets/hero.png'

export const Page = Object.freeze({
    LOGIN: "login",
    HOME: "home",
    DISPLAY: "display",
    ERROR: "error"
});

export class MyElement extends LitElement {
    static get properties() {
        return {            
            results: { type: Array },
            page: { type: String },
            pokemonName: {type: String},
            failed: {type: Boolean},
            errorMessage: {type: String}
        }
    }

    constructor() {
        super()
        this.results = []
        this.pokemonName = "";
        this.failed = false;
        
        this.gotoDefaultPage ();
    }

    gotoDefaultPage () {
        if (this.isLoggedIn ()) {
            this.page = Page.HOME;
            this.user = sessionStorage.getItem ("username");
        } else {
            this.page = Page.LOGIN;
        }        
    }
    
    isLoggedIn() {
        const token = sessionStorage.getItem("jwt")
        const name  = sessionStorage.getItem("username")
        return token && name
    }

    logout () {
        sessionStorage.clear ();
    }
    
    /*!
     * ====================================================================================================
     * ====================================================================================================
     * ===================================          RENDERING          ====================================
     * ====================================================================================================
     * ====================================================================================================
     */
    
    render() {        
        switch (this.page) {
        case Page.LOGIN:
            return this.renderLogin ();
        case Page.HOME:
            return this.renderHome ();
        case Page.DISPLAY:
            return this.renderPokemon ();
        default:
            this.gotoDefaultPage ();            
            return this.render ();
        }
    }

    renderLogin () {
        return html`
        <h2> Login </h2>

        ${this.failed
        ? html`<p class="error">this.errorMessage</p>`
        : null}

        <form id="form" @submit=${this.onLoginSubmit}>
        <input placeholder="login" id="login">
        <input placeholder="password" id="password" type="password">
        <button type="submit" @click=${this.onLoginSubmit}>Login</button>
        </form>
        `
    }
    
    renderHome () {
        return html`
        ${this.failed
        ? html`<p class="error">this.errorMessage</p>`
        : null}

        <form id="form" @submit=${this.onPokemonSubmit}>
        <input @input=${this.onPokemonInput} placeholder="Search Pokémon" list="suggestions">
        <datalist id="suggestions">
          ${this.results.map(name => html`<option>${name}</option>`)}
        </datalist>
        </form>      
    `;    
    }

    renderPokemon() {
        return html`
         <h2>Pokémon details: ${this.pokemonName}</h2>
         <button @click=${() => this.page = Page.HOME}>Back</button>
         `;
    }

    /*!
     * ====================================================================================================
     * ====================================================================================================
     * =====================================          EVENTS          =====================================
     * ====================================================================================================
     * ====================================================================================================
     */

    async onLoginSubmit (e) {
        console.log ("LOGIN?");
        e.preventDefault ();

        const login = this.renderRoot.querySelector("#login")
        const password = this.renderRoot.querySelector("#password")
        
        const loginV = login.value;
        const passwordV = password.value;
        
        const response = await fetch("/api/login", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ loginV, passwordV })
        });
        
        if (!response.ok) {
            const err = await response.json();

            this.failed = true;
            this.errorMessage = err.message || "Login failed";

            login.value = "";
            password.value = "";
                        
            return;
        }
        
        const result = await response.json ();
        sessionStorage.setItem ("jwt", result["jwt"]);
        sessionStorage.setItem ("username", result["username"]);
        this.page = Page.HOME;
    }
    
    async onPokemonInput(e) {        
        const value = e.target.value;
        if (value.length < 1) return;

        const response = await fetch (`api/pokemon-suggest?start=${value}`);
        const result = await response.json ();        

        this.results = result ["names"];
    }

    async onPokemonSubmit(e) {
        e.preventDefault ();
        const input = this.renderRoot.querySelector("input")        

        const response = await fetch (`api/pokemon-info?name=${input.value}`);
        if (!response.ok) {
            this.failed = true;
            this.errorMessage = "Please enter a valid Pokémon name";
            this.results = [];
            return;
        }
        
        const result = await response.json ();                
        this.page = Page.DISPLAY;
        this.pokemonName = input.value;        
    }

    /*!
     * ====================================================================================================
     * ====================================================================================================
     * =====================================          STYLES          =====================================
     * ====================================================================================================
     * ====================================================================================================
     */
    
    static get styles() {
        return css`
          input { padding: 8px; font-size: 1rem; }
          .error {
            color: red;
          }
        `;
    }
}

window.customElements.define('pokemon-search', MyElement)
