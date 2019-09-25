import React, {Component} from 'react';
import { withRouter } from 'react-router-dom';
import './Admin.css';
import MessagesLoader from "./MessagesLoader";

class Admin extends Component {
  /* Prevent error: perform a React state update on an unmounted
   * component. This is a no-op, but it indicates a memory leak in
   * your application. To fix, cancel all subscriptions and
   * asynchronous tasks in the componentWillUnmount method. */
  _isMounted = false;

  constructor(props) {
    super(props);
    this.state = {
      messages: MessagesLoader.Empty
    };
  }

  async componentDidMount() {
    this._isMounted = true;
    try {
      const resp = await fetch("/admin");
      if (resp.status === 401) {
        console.log("Login needed");
        this.props.history.push("/login/admin")
      }
    } catch (e) {
      console.log("Error: " + JSON.stringify(e));
    }

    try {
      if (this._isMounted) {
        this.setState({
          messages: await new MessagesLoader().load([
            { key: 'adminMenu'},
            { key: 'siteMaintenance'}
          ])
        });
      }
    } catch (e) {
      console.log("Error: " + JSON.stringify(e));
    }
  }

  componentWillUnmount() {
    this._isMounted = false;
  }

  msg = (key) => {
    return this.state.messages(key);
  }

  startSiteMaintenance = () => {
    this.props.history.push("/site")
  }

  render() {
    const body = () => {
      return (
        <nav className="panel">
          <p className="panel-heading">
            {this.msg('adminMenu')}
          </p>
          <p className="panel-block" >
            <a href="#siteMaintenance" className="is-fullwidth" onClick={this.startSiteMaintenance}>
              {this.msg('siteMaintenance')}
            </a>
          </p>
        </nav>
      );
    };

    return (
      <div className="admin">
        { body() }
      </div>
    );
  }
}

export default withRouter(Admin);
